package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.FoodsharingApiSession;
import ch.it4user.foodsharing.repository.FoodsharingApiSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
public class FoodsharingClient {
    private final RestClient restClient;
    private final AppProperties appProperties;
    private final FoodsharingApiSessionRepository sessionRepository;
    private final CryptoService cryptoService;

    public FoodsharingClient(AppProperties appProperties, FoodsharingApiSessionRepository sessionRepository, CryptoService cryptoService) {
        this.appProperties = appProperties;
        this.sessionRepository = sessionRepository;
        this.cryptoService = cryptoService;
        this.restClient = RestClient.builder().baseUrl(appProperties.getFoodsharing().getBaseUrl()).build();
    }

    public FoodsharingUserInfo getUser(String foodsharingId) {
        Map<?, ?> body = authenticatedGet("/api/users/{userId}", foodsharingId, false);
        Object id = body.get("id");
        Object name = body.get("name");
        Object sleeping = body.get("isSleeping");
        return new FoodsharingUserInfo(String.valueOf(id == null ? foodsharingId : id), String.valueOf(name == null ? foodsharingId : name), Boolean.TRUE.equals(sleeping));
    }

    public void sendMessage(String foodsharingId, String body) {
        Map<?, ?> lookup = authenticatedPost("/api/conversations/lookup", Map.of("ids", List.of(Long.valueOf(foodsharingId))), false);
        Object conversationId = Optional.ofNullable(lookup.get("id")).orElse(lookup.get("conversationId"));
        if (conversationId == null) throw new IllegalStateException("Foodsharing conversation lookup did not return an id");
        authenticatedPost("/api/conversations/{conversationId}/messages", Map.of("body", body), false, conversationId);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> authenticatedGet(String path, Object uriVariable, boolean retried) {
        SessionData session = session();
        try {
            return restClient.get().uri(path, uriVariable).headers(h -> applySession(h, session)).retrieve().body(Map.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED && !retried) { authenticate(); return authenticatedGet(path, uriVariable, true); }
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> authenticatedPost(String path, Object request, boolean retried, Object... uriVariables) {
        SessionData session = session();
        try {
            return restClient.post().uri(path, uriVariables).contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> applySession(h, session)).body(request).retrieve().body(Map.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED && !retried) { authenticate(); return authenticatedPost(path, request, true, uriVariables); }
            throw ex;
        }
    }

    private void applySession(HttpHeaders headers, SessionData session) {
        if (session.cookie() != null && !session.cookie().isBlank()) headers.set(HttpHeaders.COOKIE, session.cookie());
        if (session.csrfToken() != null && !session.csrfToken().isBlank()) headers.set("X-CSRF-Token", session.csrfToken());
    }

    private SessionData session() {
        return sessionRepository.findAll().stream().findFirst()
                .map(s -> new SessionData(cryptoService.decrypt(s.getSessionCookieCiphertext()), cryptoService.decrypt(s.getCsrfTokenCiphertext())))
                .orElseGet(this::authenticate);
    }

    @Transactional
    public SessionData authenticate() {
        var response = restClient.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", appProperties.getFoodsharing().getAdminUser(), "password", appProperties.getFoodsharing().getAdminPassword(), "rememberMe", true))
                .retrieve().toEntity(Map.class);
        String cookie = String.join("; ", response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE));
        String csrf = response.getHeaders().getFirst("X-CSRF-Token");
        if ((csrf == null || csrf.isBlank()) && response.getBody() != null && response.getBody().get("csrfToken") != null) csrf = String.valueOf(response.getBody().get("csrfToken"));
        FoodsharingApiSession session = sessionRepository.findAll().stream().findFirst().orElseGet(FoodsharingApiSession::new);
        session.setSessionCookieCiphertext(cryptoService.encrypt(cookie));
        session.setCsrfTokenCiphertext(cryptoService.encrypt(csrf));
        session.setAuthenticatedAt(Instant.now());
        sessionRepository.save(session);
        return new SessionData(cookie, csrf);
    }

    private record SessionData(String cookie, String csrfToken) {}
}
