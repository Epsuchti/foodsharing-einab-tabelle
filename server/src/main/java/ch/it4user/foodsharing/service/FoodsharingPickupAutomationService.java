package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingPickupAutomationAudit;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.FoodsharingPickupAutomationDecision;
import ch.it4user.foodsharing.repository.FoodsharingAdminConnectionRepository;
import ch.it4user.foodsharing.repository.FoodsharingPickupAutomationAuditRepository;
import ch.it4user.foodsharing.repository.FoodsharingStoreAutomationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodsharingPickupAutomationService {
    private final AppProperties appProperties;
    private final CurrentActorService currentActorService;
    private final CryptoService cryptoService;
    private final FoodsharingPickupApiClient client;
    private final FoodsharingAdminConnectionRepository connectionRepository;
    private final FoodsharingStoreAutomationRepository automationRepository;
    private final FoodsharingPickupAutomationAuditRepository auditRepository;

    public FoodsharingPickupAutomationService(AppProperties appProperties, CurrentActorService currentActorService, CryptoService cryptoService, FoodsharingPickupApiClient client, FoodsharingAdminConnectionRepository connectionRepository, FoodsharingStoreAutomationRepository automationRepository, FoodsharingPickupAutomationAuditRepository auditRepository) {
        this.appProperties = appProperties; this.currentActorService = currentActorService; this.cryptoService = cryptoService; this.client = client; this.connectionRepository = connectionRepository; this.automationRepository = automationRepository; this.auditRepository = auditRepository;
    }

    @Transactional
    public ConnectionStatus connect(String email, String password) {
        Teacher admin = currentActorService.requireTeacher();
        var session = client.login(email, password);
        FoodsharingAdminConnection c = connectionRepository.findByAdminUser(admin).orElseGet(FoodsharingAdminConnection::new);
        c.setAdminUser(admin); c.setFoodsharingEmail(email); c.setFoodsharingUserId(session.foodsharingUserId()); c.setSessionCookieCiphertext(cryptoService.encrypt(session.cookie())); c.setCsrfTokenCiphertext(cryptoService.encrypt(session.csrf())); c.setAuthenticatedAt(Instant.now());
        connectionRepository.save(c);
        return status(c);
    }

    @Transactional
    public void disconnect() { connectionRepository.findByAdminUser(currentActorService.requireTeacher()).ifPresent(connectionRepository::delete); }
    public ConnectionStatus status() { return connectionRepository.findByAdminUser(currentActorService.requireTeacher()).map(this::status).orElse(new ConnectionStatus(false, null, null, null)); }
    private ConnectionStatus status(FoodsharingAdminConnection c) { return new ConnectionStatus(true, c.getFoodsharingEmail(), c.getFoodsharingUserId(), c.getAuthenticatedAt()); }

    @Transactional
    public List<StoreAutomationView> stores() {
        FoodsharingAdminConnection c = requireConnection();
        List<FoodsharingPickupModels.Store> stores = client.stores(c);
        List<StoreAutomationView> views = new ArrayList<>();
        for (var s : stores) {
            FoodsharingStoreAutomation a = automationRepository.findByAdminConnectionAndStoreId(c, s.id()).orElseGet(() -> { var n = new FoodsharingStoreAutomation(); n.setAdminConnection(c); n.setStoreId(s.id()); return n; });
            a.setStoreName(s.name()); automationRepository.save(a); views.add(view(a));
        }
        return views;
    }

    @Transactional
    public StoreAutomationView save(long storeId, StoreAutomationRequest request) {
        FoodsharingAdminConnection c = requireConnection();
        FoodsharingStoreAutomation a = automationRepository.findByAdminConnectionAndStoreId(c, storeId).orElseGet(() -> { var n = new FoodsharingStoreAutomation(); n.setAdminConnection(c); n.setStoreId(storeId); return n; });
        a.setStoreName(request.storeName() == null ? a.getStoreName() : request.storeName()); a.setEnabled(request.enabled()); a.setGapRuleEnabled(request.gapRuleEnabled()); a.setMinimumGapDays(Math.max(0, request.minimumGapDays())); a.setCleaningRuleEnabled(request.cleaningRuleEnabled());
        return view(automationRepository.save(a));
    }

    public List<AuditView> audit() { return auditRepository.findTop100ByAdminConnectionOrderByCreatedAtDesc(requireConnection()).stream().map(this::view).toList(); }

    public List<StorePickupUserView> futurePickupUsers() {
        FoodsharingAdminConnection connection = requireConnection();
        Map<String, StorePickupUserBuilder> users = new LinkedHashMap<>();
        Instant now = Instant.now();
        for (FoodsharingPickupModels.Store store : client.stores(connection)) {
            for (FoodsharingPickupModels.Pickup pickup : client.pickups(connection, store.id())) {
                if (pickup.date().isBefore(now)) {
                    continue;
                }
                for (FoodsharingPickupModels.PickupUser pickupUser : pickup.users()) {
                    if (pickupUser.id().isBlank()) {
                        continue;
                    }
                    StorePickupUserBuilder builder = users.computeIfAbsent(pickupUser.id(), id -> new StorePickupUserBuilder(id, pickupUser.name()));
                    builder.addPickup(new StorePickupView(store.id(), store.name(), pickup.date(), pickupUser.confirmed()));
                }
            }
        }
        return users.values().stream()
                .map(StorePickupUserBuilder::build)
                .sorted(Comparator.comparingInt(StorePickupUserView::futurePickupCount).reversed()
                        .thenComparing(StorePickupUserView::name, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(StorePickupUserView::foodsharingUserId))
                .toList();
    }

    @Transactional
    public FoodsharingPickupModels.RunResult run(boolean dryRun) { return run(automationRepository.findAllByEnabledTrue(), dryRun || appProperties.getFoodsharing().getAutomation().isDryRun()); }

    @Scheduled(fixedDelayString = "#{T(java.time.Duration).parse('${app.foodsharing.automation.poll-interval:PT5M}').toMillis()}")
    public void scheduledRun() { if (appProperties.getFoodsharing().getAutomation().isEnabled()) run(appProperties.getFoodsharing().getAutomation().isDryRun()); }

    private FoodsharingPickupModels.RunResult run(List<FoodsharingStoreAutomation> automations, boolean dryRun) {
        int evaluated=0, confirmed=0, declined=0, failed=0; List<String> messages = new ArrayList<>();
        for (FoodsharingStoreAutomation a : automations) for (var p : client.pickups(a.getAdminConnection(), a.getStoreId())) for (var u : p.users()) if (!u.confirmed()) {
            evaluated++;
            try {
                var decision = evaluate(a, p.date(), u.id()); String reason = String.join("\n", decision.reasons());
                if (decision.allowed()) { if (!dryRun) client.confirm(a.getAdminConnection(), a.getStoreId(), p.date(), u.id()); confirmed++; saveAudit(a, u.id(), p.date(), dryRun, dryRun ? FoodsharingPickupAutomationDecision.WOULD_CONFIRM : FoodsharingPickupAutomationDecision.CONFIRMED, reason, null); }
                else { if (!dryRun) client.decline(a.getAdminConnection(), a.getStoreId(), p.date(), u.id(), reason); declined++; saveAudit(a, u.id(), p.date(), dryRun, dryRun ? FoodsharingPickupAutomationDecision.WOULD_DECLINE : FoodsharingPickupAutomationDecision.DECLINED, reason, null); }
            } catch (RuntimeException ex) { failed++; messages.add(ex.getMessage()); saveAudit(a, u.id(), p.date(), dryRun, FoodsharingPickupAutomationDecision.FAILED, "Automation failed", ex.getMessage()); }
        }
        return new FoodsharingPickupModels.RunResult(evaluated, confirmed, declined, failed, dryRun, messages);
    }

    private FoodsharingPickupModels.Decision evaluate(FoodsharingStoreAutomation a, Instant pickupDate, String userId) {
        List<String> reasons = new ArrayList<>();
        if (a.isGapRuleEnabled()) {
            Duration min = Duration.ofDays(a.getMinimumGapDays());
            var sameStore = new ArrayList<FoodsharingPickupModels.UserPickup>(); sameStore.addAll(client.pastPickups(a.getAdminConnection(), userId)); sameStore.addAll(client.registeredPickups(a.getAdminConnection(), userId));
            sameStore.stream().filter(p -> p.storeId() == a.getStoreId()).map(FoodsharingPickupModels.UserPickup::date).map(d -> Duration.between(d, pickupDate).abs()).filter(d -> d.compareTo(min) < 0).findAny().ifPresent(d -> reasons.add("Pickup gap is " + d.toDays() + " days; required minimum is " + a.getMinimumGapDays() + " days."));
        }
        if (a.isCleaningRuleEnabled()) {
            long cleaningStoreId = appProperties.getFoodsharing().getAutomation().getCleaningStoreId();
            if (cleaningStoreId <= 0) reasons.add("Cleaning store id is not configured.");
            else {
                Instant now = Instant.now();
                boolean past = client.pastPickups(a.getAdminConnection(), userId).stream().anyMatch(p -> p.storeId() == cleaningStoreId && !p.date().isBefore(now.minus(Duration.ofDays(183))));
                boolean future = client.registeredPickups(a.getAdminConnection(), userId).stream().anyMatch(p -> p.storeId() == cleaningStoreId && !p.date().isBefore(now) && !p.date().isAfter(now.plus(Duration.ofDays(14))));
                if (!past && !future) reasons.add("No pickup in cleaning store " + cleaningStoreId + " in the last 6 months and no planned cleaning pickup in the next 2 weeks.");
            }
        }
        if (reasons.isEmpty()) reasons.add("All enabled pickup automation rules passed.");
        return new FoodsharingPickupModels.Decision(reasons.size() == 1 && reasons.get(0).startsWith("All enabled"), reasons);
    }

    private void saveAudit(FoodsharingStoreAutomation a, String userId, Instant pickupDate, boolean dryRun, FoodsharingPickupAutomationDecision decision, String reasons, String error) { var audit = new FoodsharingPickupAutomationAudit(); audit.setAdminConnection(a.getAdminConnection()); audit.setStoreId(a.getStoreId()); audit.setFoodsharingUserId(userId); audit.setPickupDate(pickupDate); audit.setDryRun(dryRun); audit.setDecision(decision); audit.setReasons(reasons); audit.setFoodsharingError(error); auditRepository.save(audit); }
    private FoodsharingAdminConnection requireConnection() { return connectionRepository.findByAdminUser(currentActorService.requireTeacher()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED)); }
    private StoreAutomationView view(FoodsharingStoreAutomation a) { return new StoreAutomationView(a.getStoreId(), a.getStoreName(), a.isEnabled(), a.isGapRuleEnabled(), a.getMinimumGapDays(), a.isCleaningRuleEnabled()); }
    private AuditView view(FoodsharingPickupAutomationAudit a) { return new AuditView(a.getStoreId(), a.getFoodsharingUserId(), a.getPickupDate(), a.isDryRun(), a.getDecision().name(), a.getReasons(), a.getFoodsharingError(), a.getCreatedAt()); }

    public record ConnectionStatus(boolean connected, String email, String foodsharingUserId, Instant authenticatedAt) {}
    public record StoreAutomationRequest(String storeName, boolean enabled, boolean gapRuleEnabled, int minimumGapDays, boolean cleaningRuleEnabled) {}
    public record StoreAutomationView(long storeId, String storeName, boolean enabled, boolean gapRuleEnabled, int minimumGapDays, boolean cleaningRuleEnabled) {}
    public record AuditView(long storeId, String foodsharingUserId, Instant pickupDate, boolean dryRun, String decision, String reasons, String error, Instant createdAt) {}
    public record StorePickupUserView(String foodsharingUserId, String name, int futurePickupCount, List<StorePickupView> futurePickups) {}
    public record StorePickupView(long storeId, String storeName, Instant pickupDate, boolean confirmed) {}

    private static final class StorePickupUserBuilder {
        private final String foodsharingUserId;
        private String name;
        private final List<StorePickupView> futurePickups = new ArrayList<>();

        private StorePickupUserBuilder(String foodsharingUserId, String name) {
            this.foodsharingUserId = foodsharingUserId;
            this.name = name;
        }

        private void addPickup(StorePickupView pickup) {
            futurePickups.add(pickup);
        }

        private StorePickupUserView build() {
            List<StorePickupView> sortedPickups = futurePickups.stream()
                    .sorted(Comparator.comparing(StorePickupView::pickupDate))
                    .toList();
            return new StorePickupUserView(foodsharingUserId, name, sortedPickups.size(), sortedPickups);
        }
    }
}
