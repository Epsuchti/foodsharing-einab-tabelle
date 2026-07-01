package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.AuthSession;
import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.LoginToken;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.openapi.model.AuthResponse;
import ch.it4user.foodsharing.openapi.model.MessageResponse;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import ch.it4user.foodsharing.repository.AuthSessionRepository;
import ch.it4user.foodsharing.repository.LoginTokenRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final AppProperties appProperties;
    private final TeacherRepository teacherRepository;
    private final BookingUserRepository bookingUserRepository;

    public AuthService(LoginTokenRepository loginTokenRepository,
                       AuthSessionRepository authSessionRepository,
                       RoleResolutionService roleResolutionService,
                       TokenService tokenService,
                       EmailService emailService,
                       EmailTemplateService emailTemplateService,
                       AppProperties appProperties,
                       TeacherRepository teacherRepository,
                       BookingUserRepository bookingUserRepository) {
        this.loginTokenRepository = loginTokenRepository;
        this.authSessionRepository = authSessionRepository;
        this.roleResolutionService = roleResolutionService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.appProperties = appProperties;
        this.teacherRepository = teacherRepository;
        this.bookingUserRepository = bookingUserRepository;
    }

    @Transactional
    public MessageResponse requestLogin(String foodsharingId) {
        LoginTarget loginTarget = resolveLoginTarget(foodsharingId);
        String rawToken = tokenService.generateToken();
        LoginToken loginToken = new LoginToken();
        loginToken.setEmail(loginTarget.email());
        loginToken.setFoodsharingId(loginTarget.foodsharingId());
        loginToken.setTokenHash(tokenService.hash(rawToken));
        loginToken.setExpiresAt(Instant.now().plus(appProperties.getAuth().getLoginTokenValidityMinutes(), ChronoUnit.MINUTES));
        loginTokenRepository.save(loginToken);

        String loginLink = appProperties.getFrontend().getBaseUrl() + "/verify-login?token=" + rawToken;
        emailService.send(
                loginTarget.email(),
                emailTemplateService.loginSubject(loginTarget.language()),
                emailTemplateService.loginBody(loginTarget.language(), loginLink));

        MessageResponse response = new MessageResponse();
        response.setMessage("LOGIN_LINK_SENT");
        response.setDeliveryTarget(maskEmail(loginTarget.email()));
        return response;
    }

    @Transactional
    public AuthResponse verifyToken(String token) {
        LoginToken loginToken = loginTokenRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_LOGIN_TOKEN));
        if (loginToken.getUsedAt() != null || loginToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.LOGIN_TOKEN_EXPIRED_OR_USED);
        }

        Set<UserRole> roles = roleResolutionService.resolveRoles(loginToken.getFoodsharingId());
        if (roles.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_NOT_FOUND);
        }

        loginToken.setUsedAt(Instant.now());
        String rawAuthToken = tokenService.generateToken();
        AuthSession authSession = new AuthSession();
        authSession.setEmail(loginToken.getEmail());
        authSession.setFoodsharingId(loginToken.getFoodsharingId());
        authSession.setTokenHash(tokenService.hash(rawAuthToken));
        authSession.setRoles(roles.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
        authSession.setExpiresAt(Instant.now().plus(appProperties.getAuth().getTokenValidityDays(), ChronoUnit.DAYS));
        authSessionRepository.save(authSession);

        String displayName = teacherRepository.findByFoodsharingIdIgnoreCase(loginToken.getFoodsharingId())
                .map(Teacher::getName)
                .orElseGet(() -> bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(loginToken.getFoodsharingId())
                        .map(BookingUser::getName)
                        .orElse(loginToken.getFoodsharingId()));

        AuthResponse response = new AuthResponse();
        response.setAuthToken(rawAuthToken);
        response.setExpiresAt(OffsetDateTime.ofInstant(authSession.getExpiresAt(), ZoneOffset.UTC));
        response.setEmail(authSession.getEmail());
        response.setFoodsharingId(authSession.getFoodsharingId());
        response.setDisplayName(displayName);
        response.setRoles(roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(role -> ch.it4user.foodsharing.openapi.model.UserRole.fromValue(role.name()))
                .toList());
        return response;
    }

    private LoginTarget resolveLoginTarget(String foodsharingId) {
        String normalizedFoodsharingId = foodsharingId.trim();
        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizedFoodsharingId)
                .map(user -> new LoginTarget(
                        user.getFoodsharingId(),
                        user.getEmail(),
                        user.getPreferredLanguage()))
                .or(() -> teacherRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId)
                        .map(teacher -> new LoginTarget(
                                teacher.getFoodsharingId(),
                                teacher.getEmail(),
                                teacher.getPreferredLanguage())))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_NOT_FOUND));
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String masked = localPart.charAt(0) + "*".repeat(Math.max(1, localPart.length() - 1));
        return masked + domain;
    }

    private record LoginTarget(String foodsharingId, String email, LanguageCode language) {
    }
}
