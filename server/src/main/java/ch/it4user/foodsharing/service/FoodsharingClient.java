package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.FoodsharingApiSession;
import ch.it4user.foodsharing.repository.FoodsharingApiSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
public class FoodsharingClient {
    private static final Logger log = LoggerFactory.getLogger(FoodsharingClient.class);
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
            logRequest("GET", path, uriVariable);
            return restClient.get().uri(path, uriVariable).headers(h -> applySession(h, session)).retrieve().body(Map.class);
        } catch (HttpClientErrorException ex) {
            if (shouldReauthenticate(ex, retried)) { authenticate(); return authenticatedGet(path, uriVariable, true); }
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> authenticatedPost(String path, Object request, boolean retried, Object... uriVariables) {
        SessionData session = session();
        try {
            logRequest("POST", path, uriVariables);
            return restClient.post().uri(path, uriVariables).contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> applySession(h, session)).body(request).retrieve().body(Map.class);
        } catch (HttpClientErrorException ex) {
            if (shouldReauthenticate(ex, retried)) { authenticate(); return authenticatedPost(path, request, true, uriVariables); }
            throw ex;
        }
    }

    private boolean shouldReauthenticate(HttpClientErrorException ex, boolean retried) {
        if (retried) {
            return false;
        }
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return true;
        }
        return ex.getStatusCode() == HttpStatus.BAD_REQUEST
                && ex.getResponseBodyAsString() != null
                && ex.getResponseBodyAsString().contains("CSRF Failed");
    }

    private void applySession(HttpHeaders headers, SessionData session) {
        if (session.cookie() != null && !session.cookie().isBlank()) headers.set(HttpHeaders.COOKIE, session.cookie());
        if (session.csrfToken() != null && !session.csrfToken().isBlank()) headers.set("X-CSRF-Token", session.csrfToken());
    }

    private SessionData session() {
        return sessionRepository.findFirstByOrderByAuthenticatedAtDesc()
                .map(s -> new SessionData(cryptoService.decrypt(s.getSessionCookieCiphertext()), cryptoService.decrypt(s.getCsrfTokenCiphertext())))
                .orElseGet(this::authenticate);
    }

    @Transactional
    public SessionData authenticate() {
        log.info("Foodsharing request: POST /api/login");
        var response = restClient.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", appProperties.getFoodsharing().getAdminUser(), "password", appProperties.getFoodsharing().getAdminPassword(), "rememberMe", true))
                .retrieve().toEntity(Map.class);
        SessionData sessionData = extractSessionData(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), response.getBody());
        sessionRepository.deleteAll();
        FoodsharingApiSession session = new FoodsharingApiSession();
        session.setSessionCookieCiphertext(cryptoService.encrypt(sessionData.cookie()));
        session.setCsrfTokenCiphertext(cryptoService.encrypt(sessionData.csrfToken()));
        session.setAuthenticatedAt(Instant.now());
        sessionRepository.save(session);
        return sessionData;
    }

    private void logRequest(String method, String path, Object... uriVariables) {
        String resolvedPath = path;
        for (Object uriVariable : uriVariables) {
            resolvedPath = resolvedPath.replaceFirst("\\{[^}]+\\}", String.valueOf(uriVariable));
        }
        log.info("Foodsharing request: {} {}", method, resolvedPath);
    }

    private SessionData extractSessionData(List<String> setCookies, Map<?, ?> responseBody) {
        List<String> cookiePairs = new ArrayList<>();
        String csrf = null;
        for (String setCookie : setCookies) {
            String pair = setCookie.split(";", 2)[0];
            cookiePairs.add(pair);
            if (pair.startsWith("FS_CSRF_TOKEN=")) {
                csrf = pair.substring("FS_CSRF_TOKEN=".length());
            }
        }
        if ((csrf == null || csrf.isBlank()) && responseBody != null && responseBody.get("csrfToken") != null) {
            csrf = String.valueOf(responseBody.get("csrfToken"));
        }
        return new SessionData(String.join("; ", cookiePairs), csrf);
    }

    private record SessionData(String cookie, String csrfToken) {}
}
