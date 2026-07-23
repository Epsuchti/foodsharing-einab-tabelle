package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.repository.FoodsharingAdminConnectionRepository;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
public class FoodsharingPickupApiClient {
    private static final Logger log = LoggerFactory.getLogger(FoodsharingPickupApiClient.class);
    private final RestClient restClient;
    private final CryptoService cryptoService;
    private final FoodsharingAdminConnectionRepository connectionRepository;

    public FoodsharingPickupApiClient(AppProperties appProperties, CryptoService cryptoService, FoodsharingAdminConnectionRepository connectionRepository) {
        this.cryptoService = cryptoService;
        this.connectionRepository = connectionRepository;
        this.restClient = RestClient.builder().baseUrl(appProperties.getFoodsharing().getBaseUrl()).build();
    }

    @SuppressWarnings("unchecked")
    public Session login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Foodsharing email and password are required."));
        }
        log.info("Foodsharing request: POST /api/login user={}", email);
        var response = restClient.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password, "rememberMe", true))
                .retrieve().toEntity(Map.class);
        String cookie = extractCookieHeader(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE));
        Map<?, ?> body = response.getBody();
        String csrf = extractCsrfToken(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), response.getHeaders().getFirst("X-CSRF-Token"), body);
        String userId = extractUserId(body);
        if (csrf == null || csrf.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.UNEXPECTED_ERROR, List.of("Foodsharing login did not return a CSRF token."));
        }
        log.info("Foodsharing auth established user={} cookie={} csrf={}", email, fingerprint(cookie), fingerprint(csrf));
        return new Session(cookie, csrf, userId);
    }

    @SuppressWarnings("unchecked")
    public List<FoodsharingPickupModels.Store> stores(FoodsharingAdminConnection connection) {
        Object body = get(connection, "GET", "/api/users/current/stores?excludeInactive=true", Object.class, false);
        return asList(body).stream()
                .map(v -> (Map<?, ?>) v)
                .map(m -> new FoodsharingPickupModels.Store(
                        longValue(first(m, "id", "storeId")),
                        string(first(m, "name", "title")),
                        Boolean.TRUE.equals(first(m, "isManaging", "managing"))))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<FoodsharingPickupModels.Pickup> pickups(FoodsharingAdminConnection connection, long storeId) {
        Object body = get(connection, "GET", "/api/stores/{storeId}/pickups", Object.class, false, storeId);
        List<FoodsharingPickupModels.Pickup> result = new ArrayList<>();
        for (Object item : asList(body)) {
            Map<?, ?> pickup = (Map<?, ?>) item;
            Instant date = instant(first(pickup, "date", "pickupDate"));
            List<FoodsharingPickupModels.PickupUser> users = new ArrayList<>();
            for (Object userSlot : asList(first(pickup, "occupiedSlots", "users"))) {
                Map<?, ?> slot = (Map<?, ?>) userSlot;
                Map<?, ?> profile = first(slot, "profile") instanceof Map<?, ?> p ? p : slot;
                users.add(new FoodsharingPickupModels.PickupUser(string(first(profile, "id", "userId")), string(first(profile, "name")), Boolean.TRUE.equals(first(slot, "isConfirmed", "confirmed"))));
            }
            result.add(new FoodsharingPickupModels.Pickup(storeId, date, users));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<FoodsharingPickupModels.StoreMember> members(FoodsharingAdminConnection connection, long storeId) {
        Object body = get(connection, "GET", "/api/stores/{storeId}/members", Object.class, false, storeId);
        List<FoodsharingPickupModels.StoreMember> result = new ArrayList<>();
        for (Object item : asList(body)) {
            Map<?, ?> member = (Map<?, ?>) item;
            result.add(new FoodsharingPickupModels.StoreMember(
                    longValue(first(member, "id", "userId")),
                    string(first(member, "name", "firstName")),
                    instantOrNull(first(member, "lastFetch", "lastFetchedAt", "lastPickupAt")),
                    intValue(first(member, "fetchCount"))));
        }
        return result;
    }

    public void confirm(FoodsharingAdminConnection connection, long storeId, Instant pickupDate, String userId) {
        runWithSessionRetry(connection, false, () -> {
            log.info("Foodsharing request: PATCH /api/stores/{}/pickups/{}/users/{} user={} cookie={} csrf={}",
                    storeId, format(pickupDate), userId, connection.getFoodsharingEmail(),
                    fingerprint(cookie(connection)),
                    fingerprint(csrfToken(connection)));
            restClient.patch().uri("/api/stores/{storeId}/pickups/{pickupDate}/users/{userId}", storeId, format(pickupDate), userId)
                    .headers(h -> apply(h, connection)).retrieve().toBodilessEntity();
            return null;
        });
    }

    public List<RequestUser> requests(FoodsharingAdminConnection connection, long storeId) {
        Object body = get(connection, "GET", "/api/stores/{storeId}/requests", Object.class, false, storeId);
        List<RequestUser> result = new ArrayList<>();
        for (Object item : asList(body)) {
            Map<?, ?> request = (Map<?, ?>) item;
            Map<?, ?> profile = first(request, "profile", "user") instanceof Map<?, ?> p ? p : request;
            result.add(new RequestUser(string(first(profile, "id", "userId")), string(first(profile, "name")), doubleOrNull(first(request, "distanceInKm"))));
        }
        return result;
    }

    public void approveRequest(FoodsharingAdminConnection connection, long storeId, String userId) {
        runWithSessionRetry(connection, false, () -> {
            log.info("Foodsharing request: PATCH /api/stores/{}/requests/{} user={} cookie={} csrf={}",
                    storeId, userId, connection.getFoodsharingEmail(), fingerprint(cookie(connection)), fingerprint(csrfToken(connection)));
            restClient.patch().uri("/api/stores/{storeId}/requests/{userId}", storeId, userId)
                    .headers(h -> apply(h, connection)).retrieve().toBodilessEntity();
            return null;
        });
    }

    public void declineRequest(FoodsharingAdminConnection connection, long storeId, String userId, String message) {
        runWithSessionRetry(connection, false, () -> {
            log.info("Foodsharing request: DELETE /api/stores/{}/requests/{} user={} cookie={} csrf={}",
                    storeId, userId, connection.getFoodsharingEmail(), fingerprint(cookie(connection)), fingerprint(csrfToken(connection)));
            restClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/stores/{storeId}/requests/{userId}", storeId, userId)
                    .contentType(MediaType.APPLICATION_JSON).headers(h -> apply(h, connection))
                    .body(Map.of("message", message)).retrieve().toBodilessEntity();
            return null;
        });
    }

    public void sendStoreChatMessage(FoodsharingAdminConnection connection, long storeId, String body) {
        runWithSessionRetry(connection, false, () -> {
            log.info("Foodsharing request: GET /api/stores/{}/permissions user={} cookie={} csrf={}",
                    storeId, connection.getFoodsharingEmail(), fingerprint(cookie(connection)), fingerprint(csrfToken(connection)));
            Map<?, ?> permissions = restClient.get().uri("/api/stores/{storeId}/permissions", storeId)
                    .headers(h -> apply(h, connection)).retrieve().body(Map.class);
            Object teamConversationId = permissions == null ? null : first(permissions, "teamConversationId");
            if (teamConversationId == null || longValue(teamConversationId) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                        List.of("Foodsharing store " + storeId + " has no team conversation."));
            }
            log.info("Foodsharing request: POST /api/conversations/{}/messages user={} cookie={} csrf={}",
                    teamConversationId, connection.getFoodsharingEmail(), fingerprint(cookie(connection)), fingerprint(csrfToken(connection)));
            restClient.post().uri("/api/conversations/{conversationId}/messages", teamConversationId)
                    .contentType(MediaType.APPLICATION_JSON).headers(h -> apply(h, connection))
                    .body(Map.of("body", body)).retrieve().toBodilessEntity();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public void sendUserMessage(FoodsharingAdminConnection connection, String userId, String body) {
        long numericUserId;
        try {
            numericUserId = Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                    List.of("Foodsharing user ID is invalid: " + userId));
        }
        runWithSessionRetry(connection, false, () -> {
            Map<?, ?> conversation = restClient.post().uri("/api/conversations/lookup")
                    .contentType(MediaType.APPLICATION_JSON).headers(headers -> apply(headers, connection))
                    .body(Map.of("ids", List.of(numericUserId))).retrieve().body(Map.class);
            long conversationId = conversation == null ? 0 : longValue(first(conversation, "id"));
            if (conversationId <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.UNEXPECTED_ERROR,
                        List.of("Foodsharing did not return a conversation for user " + userId + "."));
            }
            restClient.post().uri("/api/conversations/{conversationId}/messages", conversationId)
                    .contentType(MediaType.APPLICATION_JSON).headers(headers -> apply(headers, connection))
                    .body(Map.of("body", body)).retrieve().toBodilessEntity();
            return null;
        });
    }

    public void decline(FoodsharingAdminConnection connection, long storeId, Instant pickupDate, String userId, String message) {
        runWithSessionRetry(connection, false, () -> {
            log.info("Foodsharing request: DELETE /api/stores/{}/pickups/{}/users/{} user={} cookie={} csrf={}",
                    storeId, format(pickupDate), userId, connection.getFoodsharingEmail(),
                    fingerprint(cookie(connection)),
                    fingerprint(csrfToken(connection)));
            restClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/stores/{storeId}/pickups/{pickupDate}/users/{userId}", storeId, format(pickupDate), userId)
                    .contentType(MediaType.APPLICATION_JSON).headers(h -> apply(h, connection))
                    .body(Map.of("message", message, "sendKickMessage", true)).retrieve().toBodilessEntity();
            return null;
        });
    }

    private <T> T get(FoodsharingAdminConnection c, String method, String uri, Class<T> type, boolean retried, Object... vars) {
        return runWithSessionRetry(c, retried, () -> {
            log.info("Foodsharing request: {} {} user={} cookie={} csrf={}",
                    method,
                    String.format(uri.replace("{storeId}", "%s").replace("{pickupDate}", "%s").replace("{userId}", "%s"), vars),
                    c.getFoodsharingEmail(),
                    fingerprint(cookie(c)),
                    fingerprint(csrfToken(c)));
            return restClient.get().uri(uri, vars).headers(h -> apply(h, c)).retrieve().body(type);
        });
    }

    private <T> T runWithSessionRetry(FoodsharingAdminConnection connection, boolean retried, RequestAction<T> action) {
        try {
            return action.run();
        } catch (HttpClientErrorException ex) {
            if (shouldReauthenticate(ex, retried)) {
                reauthenticate(connection);
                return runWithSessionRetry(connection, true, action);
            }
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

    private void reauthenticate(FoodsharingAdminConnection connection) {
        String password = cryptoService.decrypt(connection.getFoodsharingPasswordCiphertext());
        if (password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Foodsharing connection expired and cannot be refreshed. Reconnect Foodsharing."));
        }
        log.info("Foodsharing automation session expired, re-authenticating admin={} foodsharingUserId={} email={}",
                connection.getAdminUser().getId(), connection.getFoodsharingUserId(), connection.getFoodsharingEmail());
        Session session = login(connection.getFoodsharingEmail(), password);
        connection.setFoodsharingUserId(session.foodsharingUserId());
        connection.setSessionCookieCiphertext(cryptoService.encrypt(session.cookie()));
        connection.setCsrfTokenCiphertext(cryptoService.encrypt(session.csrf()));
        connection.setAuthenticatedAt(Instant.now());
        connectionRepository.save(connection);
    }
    private void apply(HttpHeaders h, FoodsharingAdminConnection c) {
        String cookie = cookie(c);
        h.set(HttpHeaders.COOKIE, cookie);
        String csrf = csrfToken(c);
        if (csrf != null && !csrf.isBlank()) {
            h.set("X-CSRF-Token", csrf);
        }
    }
    private String cookie(FoodsharingAdminConnection c) { return cryptoService.decrypt(c.getSessionCookieCiphertext()); }
    private String csrfToken(FoodsharingAdminConnection c) {
        String csrf = cryptoService.decrypt(c.getCsrfTokenCiphertext());
        if (csrf != null && !csrf.isBlank()) {
            return csrf;
        }
        return cookieValue(cookie(c), "FS_CSRF_TOKEN");
    }

    @FunctionalInterface
    private interface RequestAction<T> {
        T run();
    }
    private String format(Instant i) { return i.toString().replaceAll("Z$", ".000Z").replaceAll("\\.([0-9]{3})[0-9]*Z$", ".$1Z"); }
    private static List<?> asList(Object o) { return o instanceof List<?> l ? l : List.of(); }
    private static String extractCookieHeader(List<String> setCookies) {
        List<String> cookiePairs = new ArrayList<>();
        for (String setCookie : setCookies) {
            cookiePairs.add(setCookie.split(";", 2)[0]);
        }
        return String.join("; ", cookiePairs);
    }
    private static String extractCsrfToken(List<String> setCookies, String headerToken, Map<?, ?> body) {
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken;
        }
        for (String setCookie : setCookies) {
            String pair = setCookie.split(";", 2)[0];
            if (pair.startsWith("FS_CSRF_TOKEN=")) {
                return pair.substring("FS_CSRF_TOKEN=".length());
            }
        }
        if (body != null) {
            Object csrfToken = first(body, "csrfToken", "csrf", "token");
            if (csrfToken != null && !string(csrfToken).isBlank()) {
                return string(csrfToken);
            }
        }
        return null;
    }
    private static String extractUserId(Map<?, ?> body) {
        if (body == null) {
            return null;
        }
        Object userId = first(body, "id", "userId", "foodsharingUserId");
        if (userId != null && !string(userId).isBlank()) {
            return string(userId);
        }
        Object user = first(body, "user");
        if (user instanceof Map<?, ?> nested) {
            Object nestedId = first(nested, "id", "userId");
            if (nestedId != null && !string(nestedId).isBlank()) {
                return string(nestedId);
            }
        }
        return null;
    }
    private static Object first(Map<?, ?> m, String... names) { for (String n:names) if (m.containsKey(n)) return m.get(n); return null; }
    private static String string(Object o) { return o == null ? "" : String.valueOf(o); }
    private static long longValue(Object o) { return o instanceof Number n ? n.longValue() : Long.parseLong(string(o).isBlank()?"0":string(o)); }
    private static Instant instant(Object o) { return Instant.parse(string(o)); }
    private static Instant instantOrNull(Object o) { return o == null || string(o).isBlank() ? null : Instant.parse(string(o)); }
    private static int intValue(Object o) { return o instanceof Number n ? n.intValue() : Integer.parseInt(string(o).isBlank()?"0":string(o)); }
    private static Double doubleValue(Object o) { return o instanceof Number n ? n.doubleValue() : Double.parseDouble(string(o).isBlank()?"0":string(o)); }
    private static Double doubleOrNull(Object o) { return o == null || string(o).isBlank() ? null : doubleValue(o); }
    public record RequestUser(String id, String name, Double distanceInKm) {}
    private static String cookieValue(String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (String cookie : cookieHeader.split(";\\s*")) {
            int index = cookie.indexOf('=');
            if (index > 0 && name.equals(cookie.substring(0, index))) {
                return cookie.substring(index + 1);
            }
        }
        return null;
    }
    private static String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6 && i < hash.length; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
    public record Session(String cookie, String csrf, String foodsharingUserId) {}
}
