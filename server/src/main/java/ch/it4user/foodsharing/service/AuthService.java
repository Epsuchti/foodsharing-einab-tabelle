package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.AuthSession;
import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.LoginToken;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.openapi.model.AuthResponse;
import ch.it4user.foodsharing.openapi.model.BezirkResponse;
import ch.it4user.foodsharing.openapi.model.MessageResponse;
import ch.it4user.foodsharing.repository.AuthSessionRepository;
import ch.it4user.foodsharing.repository.LoginTokenRepository;
import ch.it4user.foodsharing.repository.UserRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final LoginTokenRepository loginTokenRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PermissionResolutionService permissionResolutionService;
    private final TokenService tokenService;
    private final FoodsharingMessageService messageService;
    private final MessageTemplateService messageTemplateService;
    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final BookingUserService bookingUserService;
    private final FoodsharingClient foodsharingClient;
    private final BezirkService bezirkService;

    public AuthService(LoginTokenRepository loginTokenRepository,
                       AuthSessionRepository authSessionRepository,
                       PermissionResolutionService permissionResolutionService,
                       TokenService tokenService,
                       FoodsharingMessageService messageService,
                       MessageTemplateService messageTemplateService,
                       AppProperties appProperties,
                       UserRepository userRepository,
                       BookingUserService bookingUserService,
                       FoodsharingClient foodsharingClient,
                       BezirkService bezirkService) {
        this.loginTokenRepository = loginTokenRepository;
        this.authSessionRepository = authSessionRepository;
        this.permissionResolutionService = permissionResolutionService;
        this.tokenService = tokenService;
        this.messageService = messageService;
        this.messageTemplateService = messageTemplateService;
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.bookingUserService = bookingUserService;
        this.foodsharingClient = foodsharingClient;
        this.bezirkService = bezirkService;
    }

    @Transactional
    public MessageResponse requestLogin(String bezirkSlug, String foodsharingId) {
        Bezirk selectedBezirk = bezirkService.requireActive(bezirkSlug);
        LoginTarget loginTarget = resolveLoginTarget(foodsharingId, selectedBezirk);
        String rawToken = tokenService.generateToken();
        LoginToken loginToken = new LoginToken();
        loginToken.setFoodsharingId(loginTarget.foodsharingId());
        loginToken.setTokenHash(tokenService.hash(rawToken));
        loginToken.setExpiresAt(Instant.now().plus(appProperties.getAuth().getLoginTokenValidityMinutes(), ChronoUnit.MINUTES));
        loginTokenRepository.save(loginToken);

        String loginLink = appProperties.getFrontend().getBaseUrl()
                + "/bezirke/" + loginTarget.bezirkSlug() + "/verify-login?token=" + rawToken;
        messageService.send(
                loginTarget.foodsharingId(),
                messageTemplateService.loginSubject(loginTarget.language()),
                messageTemplateService.loginBody(loginTarget.language(), loginLink));

        MessageResponse response = new MessageResponse();
        response.setMessage("LOGIN_LINK_SENT");
        response.setDeliveryTarget("Foodsharing ID " + loginTarget.foodsharingId());
        return response;
    }

    @Transactional
    public AuthResponse verifyToken(String token) {
        LoginToken loginToken = loginTokenRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_LOGIN_TOKEN));
        if (loginToken.getUsedAt() != null || loginToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.LOGIN_TOKEN_EXPIRED_OR_USED);
        }

        loginToken.setUsedAt(Instant.now());
        return createAuthResponse(loginToken.getFoodsharingId());
    }

    public Optional<AuthResponse> authenticateFoodsharingIdIfPossible(String foodsharingId) {
        try {
            return Optional.of(createAuthResponse(foodsharingId));
        } catch (ApiException exception) {
            if (exception.getCode() == ApiErrorCode.ACCOUNT_NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private LoginTarget resolveLoginTarget(String foodsharingId, Bezirk selectedBezirk) {
        String normalizedFoodsharingId = foodsharingId.trim();
        User user = userRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizedFoodsharingId)
                .map(this::refreshPhoneNumber)
                .orElseGet(() -> bookingUserService.getOrCreate(normalizedFoodsharingId, LanguageCode.DE));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (user.getBezirk() == null && (user.isCanGiveEinAbs() || user.isWantsToBeTeacher())) {
            bookingUserService.assignToBezirk(user, selectedBezirk);
        }
        String targetBezirkSlug = user.getBezirk() == null ? selectedBezirk.getSlug() : user.getBezirk().getSlug();
        return new LoginTarget(user.getFoodsharingId(), user.getPreferredLanguage(), targetBezirkSlug);
    }

    private User refreshPhoneNumber(User user) {
        String phoneNumber = foodsharingClient.fetchPhoneNumber(user.getFoodsharingId());
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
        return user;
    }

    private AuthResponse createAuthResponse(String foodsharingId) {
        Set<UserPermission> permissions = permissionResolutionService.resolvePermissions(foodsharingId);
        User loginUser = userRepository.findByFoodsharingIdIgnoreCase(foodsharingId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_NOT_FOUND));
        if (!loginUser.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_NOT_FOUND);
        }

        String rawAuthToken = tokenService.generateToken();
        AuthSession authSession = new AuthSession();
        authSession.setFoodsharingId(foodsharingId);
        authSession.setTokenHash(tokenService.hash(rawAuthToken));
        authSession.setPermissions(permissions.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
        authSession.setExpiresAt(Instant.now().plus(appProperties.getAuth().getTokenValidityDays(), ChronoUnit.DAYS));
        authSessionRepository.save(authSession);

        String displayName = userRepository.findByFoodsharingIdIgnoreCase(foodsharingId)
                .map(User::getName)
                .orElse(foodsharingId);

        AuthResponse response = new AuthResponse();
        response.setAuthToken(rawAuthToken);
        response.setExpiresAt(OffsetDateTime.ofInstant(authSession.getExpiresAt(), ZoneOffset.UTC));
        response.setEmail(null);
        response.setFoodsharingId(authSession.getFoodsharingId());
        response.setDisplayName(displayName);
        response.setBezirk(toBezirkResponse(loginUser.getBezirk()));
        response.setPermissions(permissions.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(permission -> ch.it4user.foodsharing.openapi.model.UserPermission.fromValue(permission.name()))
                .toList());
        return response;
    }

    private BezirkResponse toBezirkResponse(Bezirk bezirk) {
        if (bezirk == null) {
            return null;
        }
        BezirkResponse response = new BezirkResponse();
        response.setId(bezirk.getId());
        response.setName(bezirk.getName());
        response.setSlug(bezirk.getSlug());
        return response;
    }

    private record LoginTarget(String foodsharingId, LanguageCode language, String bezirkSlug) {
    }
}
