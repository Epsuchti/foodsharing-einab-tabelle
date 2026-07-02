package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FoodsharingPickupApiClient {
    private static final Logger log = LoggerFactory.getLogger(FoodsharingPickupApiClient.class);
    private final RestClient restClient;
    private final CryptoService cryptoService;

    public FoodsharingPickupApiClient(AppProperties appProperties, CryptoService cryptoService) {
        this.cryptoService = cryptoService;
        this.restClient = RestClient.builder().baseUrl(appProperties.getFoodsharing().getBaseUrl()).build();
    }

    @SuppressWarnings("unchecked")
    public Session login(String email, String password) {
        log.info("Foodsharing request: POST /api/login");
        var response = restClient.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password, "rememberMe", true))
                .retrieve().toEntity(Map.class);
        String cookie = String.join("; ", response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE));
        String csrf = response.getHeaders().getFirst("X-CSRF-Token");
        Map<?, ?> body = response.getBody();
        if ((csrf == null || csrf.isBlank()) && body != null && body.get("csrfToken") != null) csrf = String.valueOf(body.get("csrfToken"));
        String userId = body == null ? null : string(body.get("id"));
        return new Session(cookie, csrf, userId);
    }

    @SuppressWarnings("unchecked")
    public List<FoodsharingPickupModels.Store> stores(FoodsharingAdminConnection connection) {
        Object body = get(connection, "GET", "/api/users/current/stores?excludeInactive=true", Object.class);
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
        Object body = get(connection, "GET", "/api/stores/{storeId}/pickups", Object.class, storeId);
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
        Object body = get(connection, "GET", "/api/stores/{storeId}/members", Object.class, storeId);
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
        log.info("Foodsharing request: PATCH /api/stores/{}/pickups/{}/users/{}", storeId, format(pickupDate), userId);
        restClient.patch().uri("/api/stores/{storeId}/pickups/{pickupDate}/users/{userId}", storeId, format(pickupDate), userId)
                .headers(h -> apply(h, connection)).retrieve().toBodilessEntity();
    }

    public void decline(FoodsharingAdminConnection connection, long storeId, Instant pickupDate, String userId, String message) {
        log.info("Foodsharing request: DELETE /api/stores/{}/pickups/{}/users/{}", storeId, format(pickupDate), userId);
        restClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/stores/{storeId}/pickups/{pickupDate}/users/{userId}", storeId, format(pickupDate), userId)
                .contentType(MediaType.APPLICATION_JSON).headers(h -> apply(h, connection))
                .body(Map.of("message", message, "sendKickMessage", true)).retrieve().toBodilessEntity();
    }

    private <T> T get(FoodsharingAdminConnection c, String method, String uri, Class<T> type, Object... vars) {
        log.info("Foodsharing request: {} {}", method, String.format(uri.replace("{storeId}", "%s").replace("{pickupDate}", "%s").replace("{userId}", "%s"), vars));
        return restClient.get().uri(uri, vars).headers(h -> apply(h, c)).retrieve().body(type);
    }
    private void apply(HttpHeaders h, FoodsharingAdminConnection c) { h.set(HttpHeaders.COOKIE, cryptoService.decrypt(c.getSessionCookieCiphertext())); h.set("X-CSRF-Token", cryptoService.decrypt(c.getCsrfTokenCiphertext())); }
    private String format(Instant i) { return i.toString().replaceAll("Z$", ".000Z").replaceAll("\\.([0-9]{3})[0-9]*Z$", ".$1Z"); }
    private static List<?> asList(Object o) { return o instanceof List<?> l ? l : List.of(); }
    private static Object first(Map<?, ?> m, String... names) { for (String n:names) if (m.containsKey(n)) return m.get(n); return null; }
    private static String string(Object o) { return o == null ? "" : String.valueOf(o); }
    private static long longValue(Object o) { return o instanceof Number n ? n.longValue() : Long.parseLong(string(o).isBlank()?"0":string(o)); }
    private static Instant instant(Object o) { return Instant.parse(string(o)); }
    private static Instant instantOrNull(Object o) { return o == null || string(o).isBlank() ? null : Instant.parse(string(o)); }
    private static int intValue(Object o) { return o instanceof Number n ? n.intValue() : Integer.parseInt(string(o).isBlank()?"0":string(o)); }
    public record Session(String cookie, String csrf, String foodsharingUserId) {}
}
