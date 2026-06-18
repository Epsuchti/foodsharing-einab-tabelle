package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.AuthSession;
import ch.it4user.foodsharing.domain.entity.LoginToken;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.openapi.model.AuthResponse;
import ch.it4user.foodsharing.repository.AuthSessionRepository;
import ch.it4user.foodsharing.repository.LoginTokenRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private final AppProperties appProperties;
    private final TeacherRepository teacherRepository;

    public AuthService(LoginTokenRepository loginTokenRepository,
                       AuthSessionRepository authSessionRepository,
                       RoleResolutionService roleResolutionService,
                       TokenService tokenService,
                       EmailService emailService,
                       AppProperties appProperties,
                       TeacherRepository teacherRepository) {
        this.loginTokenRepository = loginTokenRepository;
        this.authSessionRepository = authSessionRepository;
        this.roleResolutionService = roleResolutionService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.appProperties = appProperties;
        this.teacherRepository = teacherRepository;
    }

    @Transactional
    public void requestLogin(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String rawToken = tokenService.generateToken();
        LoginToken loginToken = new LoginToken();
        loginToken.setEmail(normalizedEmail);
        loginToken.setTokenHash(tokenService.hash(rawToken));
        loginToken.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC)
                .plusMinutes(appProperties.getAuth().getLoginTokenValidityMinutes()));
        loginTokenRepository.save(loginToken);

        String loginLink = appProperties.getFrontend().getBaseUrl() + "/verify-login?token=" + rawToken;
        emailService.send(normalizedEmail, "Your foodsharing login link",
                "Open this link to sign in:\n" + loginLink);
    }

    @Transactional
    public AuthResponse verifyToken(String token) {
        LoginToken loginToken = loginTokenRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid login token"));
        if (loginToken.getUsedAt() != null || loginToken.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Login token expired or already used");
        }

        Set<UserRole> roles = roleResolutionService.resolveRoles(loginToken.getEmail());
        if (roles.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No accessible account found for this email address");
        }

        loginToken.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
        String rawAuthToken = tokenService.generateToken();
        AuthSession authSession = new AuthSession();
        authSession.setEmail(loginToken.getEmail());
        authSession.setTokenHash(tokenService.hash(rawAuthToken));
        authSession.setRoles(roles.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
        authSession.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(appProperties.getAuth().getTokenValidityDays()));
        authSessionRepository.save(authSession);

        String displayName = teacherRepository.findByEmailIgnoreCase(loginToken.getEmail())
                .map(Teacher::getName)
                .orElse(loginToken.getEmail());

        AuthResponse response = new AuthResponse();
        response.setAuthToken(rawAuthToken);
        response.setExpiresAt(authSession.getExpiresAt());
        response.setEmail(authSession.getEmail());
        response.setDisplayName(displayName);
        response.setRoles(roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(role -> ch.it4user.foodsharing.openapi.model.UserRole.fromValue(role.name()))
                .toList());
        return response;
    }
}
