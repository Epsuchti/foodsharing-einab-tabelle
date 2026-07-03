package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.AuthSession;
import ch.it4user.foodsharing.domain.entity.LoginToken;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.openapi.model.AuthResponse;
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
    private final RoleResolutionService roleResolutionService;
    private final TokenService tokenService;
    private final FoodsharingMessageService messageService;
    private final MessageTemplateService messageTemplateService;
    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final BookingUserService bookingUserService;

    public AuthService(LoginTokenRepository loginTokenRepository,
                       AuthSessionRepository authSessionRepository,
                       RoleResolutionService roleResolutionService,
                       TokenService tokenService,
                       FoodsharingMessageService messageService,
                       MessageTemplateService messageTemplateService,
                       AppProperties appProperties,
                       UserRepository userRepository,
                       BookingUserService bookingUserService) {
        this.loginTokenRepository = loginTokenRepository;
        this.authSessionRepository = authSessionRepository;
        this.roleResolutionService = roleResolutionService;
        this.tokenService = tokenService;
        this.messageService = messageService;
        this.messageTemplateService = messageTemplateService;
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.bookingUserService = bookingUserService;
    }

    @Transactional
    public MessageResponse requestLogin(String foodsharingId) {
        LoginTarget loginTarget = resolveLoginTarget(foodsharingId);
        String rawToken = tokenService.generateToken();
        LoginToken loginToken = new LoginToken();
        loginToken.setFoodsharingId(loginTarget.foodsharingId());
        loginToken.setTokenHash(tokenService.hash(rawToken));
        loginToken.setExpiresAt(Instant.now().plus(appProperties.getAuth().getLoginTokenValidityMinutes(), ChronoUnit.MINUTES));
        loginTokenRepository.save(loginToken);

        String loginLink = appProperties.getFrontend().getBaseUrl() + "/verify-login?token=" + rawToken;
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

    private LoginTarget resolveLoginTarget(String foodsharingId) {
        String normalizedFoodsharingId = foodsharingId.trim();
        User user = userRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizedFoodsharingId)
                .orElseGet(() -> bookingUserService.getOrCreate(normalizedFoodsharingId, LanguageCode.DE));
        return new LoginTarget(user.getFoodsharingId(), user.getPreferredLanguage());
    }

    private AuthResponse createAuthResponse(String foodsharingId) {
        Set<UserRole> roles = roleResolutionService.resolveRoles(foodsharingId);
        if (roles.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_NOT_FOUND);
        }

        String rawAuthToken = tokenService.generateToken();
        AuthSession authSession = new AuthSession();
        authSession.setFoodsharingId(foodsharingId);
        authSession.setTokenHash(tokenService.hash(rawAuthToken));
        authSession.setRoles(roles.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
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
        response.setRoles(roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(role -> ch.it4user.foodsharing.openapi.model.UserRole.fromValue(role.name()))
                .toList());
        return response;
    }

    private record LoginTarget(String foodsharingId, LanguageCode language) {
    }
}
