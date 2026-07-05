package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingFuturePickupUsersCache;
import ch.it4user.foodsharing.domain.entity.FoodsharingPickupAutomationAudit;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreMembersCache;
import ch.it4user.foodsharing.domain.entity.FoodsharingStorePickupsCache;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.FoodsharingPickupAutomationDecision;
import ch.it4user.foodsharing.repository.FoodsharingAdminConnectionRepository;
import ch.it4user.foodsharing.repository.FoodsharingPickupAutomationAuditRepository;
import ch.it4user.foodsharing.repository.FoodsharingFuturePickupUsersCacheRepository;
import ch.it4user.foodsharing.repository.FoodsharingStoreMembersCacheRepository;
import ch.it4user.foodsharing.repository.FoodsharingStorePickupsCacheRepository;
import ch.it4user.foodsharing.repository.FoodsharingStoreAutomationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodsharingPickupAutomationService {
    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final Logger log = LoggerFactory.getLogger(FoodsharingPickupAutomationService.class);
    private final AppProperties appProperties;
    private final CurrentActorService currentActorService;
    private final CryptoService cryptoService;
    private final FoodsharingPickupApiClient client;
    private final FoodsharingAdminConnectionRepository connectionRepository;
    private final FoodsharingStoreAutomationRepository automationRepository;
    private final FoodsharingPickupAutomationAuditRepository auditRepository;
    private final FoodsharingFuturePickupUsersCacheRepository futurePickupUsersCacheRepository;
    private final FoodsharingStoreMembersCacheRepository storeMembersCacheRepository;
    private final FoodsharingStorePickupsCacheRepository storePickupsCacheRepository;
    private final ObjectMapper objectMapper;

    public FoodsharingPickupAutomationService(AppProperties appProperties, CurrentActorService currentActorService, CryptoService cryptoService, FoodsharingPickupApiClient client, FoodsharingAdminConnectionRepository connectionRepository, FoodsharingStoreAutomationRepository automationRepository, FoodsharingPickupAutomationAuditRepository auditRepository, FoodsharingFuturePickupUsersCacheRepository futurePickupUsersCacheRepository, FoodsharingStoreMembersCacheRepository storeMembersCacheRepository, FoodsharingStorePickupsCacheRepository storePickupsCacheRepository, ObjectMapper objectMapper) {
        this.appProperties = appProperties; this.currentActorService = currentActorService; this.cryptoService = cryptoService; this.client = client; this.connectionRepository = connectionRepository; this.automationRepository = automationRepository; this.auditRepository = auditRepository; this.futurePickupUsersCacheRepository = futurePickupUsersCacheRepository; this.storeMembersCacheRepository = storeMembersCacheRepository; this.storePickupsCacheRepository = storePickupsCacheRepository; this.objectMapper = objectMapper;
    }

    @Transactional
    public ConnectionStatus connect(String email, String password) {
        User admin = currentActorService.requireTeacher();
        var session = client.login(email, password);
        FoodsharingAdminConnection c = connectionRepository.findByAdminUser(admin).orElseGet(FoodsharingAdminConnection::new);
        c.setAdminUser(admin); c.setFoodsharingEmail(email); c.setFoodsharingUserId(session.foodsharingUserId()); c.setSessionCookieCiphertext(cryptoService.encrypt(session.cookie())); c.setCsrfTokenCiphertext(cryptoService.encrypt(session.csrf())); c.setAuthenticatedAt(Instant.now());
        connectionRepository.save(c);
        log.info("Foodsharing automation connection saved admin={} foodsharingUserId={} email={}", admin.getId(), session.foodsharingUserId(), email);
        return status(c);
    }

    @Transactional
    public void disconnect() {
        User admin = currentActorService.requireTeacher();
        connectionRepository.findByAdminUser(admin).ifPresent(connection -> {
            log.info("Foodsharing automation connection removed admin={} foodsharingUserId={} email={}", admin.getId(), connection.getFoodsharingUserId(), connection.getFoodsharingEmail());
            connectionRepository.delete(connection);
        });
    }
    public ConnectionStatus status() { return connectionRepository.findByAdminUser(currentActorService.requireTeacher()).map(this::status).orElse(new ConnectionStatus(false, null, null, null)); }
    private ConnectionStatus status(FoodsharingAdminConnection c) { return new ConnectionStatus(true, c.getFoodsharingEmail(), c.getFoodsharingUserId(), c.getAuthenticatedAt()); }

    @Transactional
    public List<StoreAutomationView> stores() {
        FoodsharingAdminConnection c = requireConnection();
        List<FoodsharingPickupModels.Store> stores = managedStores(c);
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
        a.setStoreName(request.storeName() == null ? a.getStoreName() : request.storeName()); a.setEnabled(request.enabled()); a.setGapRuleEnabled(request.gapRuleEnabled()); a.setMinimumGapDays(Math.max(0, request.minimumGapDays())); a.setCleaningRuleEnabled(request.cleaningRuleEnabled()); a.setExperienceRuleEnabled(request.experienceRuleEnabled());
        return view(automationRepository.save(a));
    }

    public List<AuditView> audit() {
        FoodsharingAdminConnection connection = requireConnection();
        Map<Long, String> storeNames = automationRepository.findAllByAdminConnection(connection).stream()
                .collect(java.util.stream.Collectors.toMap(FoodsharingStoreAutomation::getStoreId, FoodsharingStoreAutomation::getStoreName, (left, right) -> left));
        return auditRepository.findTop100ByAdminConnectionOrderByCreatedAtDesc(connection).stream().map(a -> view(a, storeNames)).toList();
    }

    @Transactional
    public List<StorePickupUserView> futurePickupUsers() {
        FoodsharingAdminConnection connection = requireConnection();
        Duration ttl = Duration.parse(appProperties.getFoodsharing().getAutomation().getFuturePickupCacheTtl());
        Instant now = Instant.now();
        var existingCache = futurePickupUsersCacheRepository.findByAdminConnection(connection);
        if (existingCache.isPresent()) {
            FoodsharingFuturePickupUsersCache cache = existingCache.get();
            if (cache.getRefreshedAt() != null && cache.getRefreshedAt().plus(ttl).isAfter(now)) {
                try {
                    return deserializeFuturePickupUsers(cache.getPayloadJson());
                } catch (RuntimeException ignored) {
                    // Fall through and refresh from Foodsharing if the cached payload is unreadable.
                }
            }
        }
        List<StorePickupUserView> fresh = loadFuturePickupUsers(connection);
        FoodsharingFuturePickupUsersCache cache = existingCache.orElseGet(FoodsharingFuturePickupUsersCache::new);
        cache.setAdminConnection(connection);
        cache.setRefreshedAt(now);
        cache.setPayloadJson(serializeFuturePickupUsers(fresh));
        futurePickupUsersCacheRepository.save(cache);
        return fresh;
    }

    private List<StorePickupUserView> loadFuturePickupUsers(FoodsharingAdminConnection connection) {
        Map<String, StorePickupUserBuilder> users = new LinkedHashMap<>();
        Instant now = Instant.now();
        for (FoodsharingPickupModels.Store store : managedStores(connection)) {
            for (FoodsharingPickupModels.Pickup pickup : storePickups(connection, store.id())) {
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

    private List<FoodsharingPickupModels.Store> managedStores(FoodsharingAdminConnection connection) {
        return client.stores(connection).stream()
                .filter(FoodsharingPickupModels.Store::isManaging)
                .toList();
    }

    private List<StorePickupUserView> deserializeFuturePickupUsers(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<List<StorePickupUserView>>() {});
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not read cached future pickup users."));
        }
    }

    private String serializeFuturePickupUsers(List<StorePickupUserView> users) {
        try {
            return objectMapper.writeValueAsString(users);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not store future pickup users cache."));
        }
    }

    private List<FoodsharingPickupModels.Pickup> deserializeStorePickups(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<List<FoodsharingPickupModels.Pickup>>() {});
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not read cached store pickups."));
        }
    }

    private String serializeStorePickups(List<FoodsharingPickupModels.Pickup> pickups) {
        try {
            return objectMapper.writeValueAsString(pickups);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not store cached store pickups."));
        }
    }

    private List<FoodsharingPickupModels.StoreMember> storeMembers(FoodsharingAdminConnection connection, long storeId) {
        Duration ttl = Duration.parse(appProperties.getFoodsharing().getAutomation().getStoreMembersCacheTtl());
        Instant now = Instant.now();
        var existingCache = storeMembersCacheRepository.findByAdminConnectionAndStoreId(connection, storeId);
        if (existingCache.isPresent()) {
            FoodsharingStoreMembersCache cache = existingCache.get();
            if (cache.getRefreshedAt() != null && cache.getRefreshedAt().plus(ttl).isAfter(now)) {
                try {
                    return deserializeStoreMembers(cache.getPayloadJson());
                } catch (RuntimeException ignored) {
                    // Fall through and refresh from Foodsharing if the cached payload is unreadable.
                }
            }
        }
        List<FoodsharingPickupModels.StoreMember> fresh = client.members(connection, storeId);
        FoodsharingStoreMembersCache cache = existingCache.orElseGet(FoodsharingStoreMembersCache::new);
        cache.setAdminConnection(connection);
        cache.setStoreId(storeId);
        cache.setRefreshedAt(now);
        cache.setPayloadJson(serializeStoreMembers(fresh));
        storeMembersCacheRepository.save(cache);
        return fresh;
    }

    private List<FoodsharingPickupModels.StoreMember> deserializeStoreMembers(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<List<FoodsharingPickupModels.StoreMember>>() {});
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not read cached store members."));
        }
    }

    private String serializeStoreMembers(List<FoodsharingPickupModels.StoreMember> members) {
        try {
            return objectMapper.writeValueAsString(members);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not store cached store members."));
        }
    }

    @Transactional
    public FoodsharingPickupModels.RunResult run(boolean dryRun) { return run(automationRepository.findAllByEnabledTrue(), dryRun || appProperties.getFoodsharing().getAutomation().isDryRun()); }

    @Scheduled(fixedDelayString = "#{T(java.time.Duration).parse('${app.foodsharing.automation.poll-interval:PT5M}').toMillis()}")
    @Transactional
    public void scheduledRun() {
        if (!appProperties.getFoodsharing().getAutomation().isEnabled()) {
            return;
        }
        boolean dryRun = appProperties.getFoodsharing().getAutomation().isDryRun();
        List<FoodsharingStoreAutomation> automations = automationRepository.findAllByEnabledTrue();
        log.info("Foodsharing automation poll started (dryRun={}, enabledAutomations={})", dryRun, automations.size());
        if (automations.isEmpty()) {
            log.info("Foodsharing automation poll skipped (dryRun={}, enabledAutomations=0)", dryRun);
            return;
        }
        FoodsharingPickupModels.RunResult result = run(automations, dryRun);
        log.info("Foodsharing automation poll finished (evaluated={}, confirmed={}, declined={}, failed={}, dryRun={})", result.evaluated(), result.confirmed(), result.declined(), result.failed(), result.dryRun());
    }

    private FoodsharingPickupModels.RunResult run(List<FoodsharingStoreAutomation> automations, boolean dryRun) {
        int evaluated=0, confirmed=0, declined=0, failed=0; List<String> messages = new ArrayList<>();
        if (automations.isEmpty()) {
            log.info("Foodsharing automation run skipped (dryRun={}, automations=0)", dryRun);
            return new FoodsharingPickupModels.RunResult(0, 0, 0, 0, dryRun, messages);
        }
        Map<java.util.UUID, FoodsharingAdminConnection> connectionsById = automations.stream()
                .map(FoodsharingStoreAutomation::getAdminConnection)
                .collect(java.util.stream.Collectors.toMap(
                        FoodsharingAdminConnection::getId,
                        connection -> connection,
                        (left, right) -> left,
                        LinkedHashMap::new));
        String connectionSummary = connectionsById.values().stream()
                .map(connection -> "admin=" + connection.getAdminUser().getId() + ",foodsharingUserId=" + connection.getFoodsharingUserId() + ",email=" + connection.getFoodsharingEmail())
                .collect(java.util.stream.Collectors.joining(" | "));
        log.info("Foodsharing automation run started (dryRun={}, automations={}, connections={})", dryRun, automations.size(), connectionSummary);
        Map<String, List<FoodsharingPickupModels.Pickup>> initialPickupsCache = new HashMap<>();
        Map<String, List<FoodsharingPickupModels.Pickup>> livePickupsCache = new HashMap<>();
        for (FoodsharingStoreAutomation a : automations) {
            String cacheKey = cacheKey(a.getAdminConnection(), a.getStoreId());
            log.info("Foodsharing automation evaluating storeId={} storeName={} admin={} foodsharingUserId={} email={}", a.getStoreId(), a.getStoreName(), a.getAdminConnection().getAdminUser().getId(), a.getAdminConnection().getFoodsharingUserId(), a.getAdminConnection().getFoodsharingEmail());
            List<FoodsharingPickupModels.Pickup> storePickups = storePickups(a.getAdminConnection(), a.getStoreId()).stream()
                    .sorted(Comparator.comparing(FoodsharingPickupModels.Pickup::date))
                    .toList();
            initialPickupsCache.putIfAbsent(cacheKey, copyPickups(storePickups));
            livePickupsCache.putIfAbsent(cacheKey, copyPickups(storePickups));
            for (var p : storePickups) for (var u : p.users()) if (!u.confirmed()) {
            evaluated++;
            try {
                var decision = evaluate(a, p.date(), u.id(), livePickupsCache, initialPickupsCache); String reason = String.join("\n", decision.reasons());
                String userMessage = decision.allowed() ? null : reason;
                if (decision.allowed()) {
                    log.info("Foodsharing automation decision=confirm dryRun={} admin={} foodsharingUserId={} email={} storeId={} pickupDate={} userId={} userName={}", dryRun, a.getAdminConnection().getAdminUser().getId(), a.getAdminConnection().getFoodsharingUserId(), a.getAdminConnection().getFoodsharingEmail(), a.getStoreId(), p.date(), u.id(), u.name());
                    if (!dryRun) client.confirm(a.getAdminConnection(), a.getStoreId(), p.date(), u.id());
                    confirmed++;
                    saveAudit(a, u.id(), u.name(), p.date(), dryRun, dryRun ? FoodsharingPickupAutomationDecision.WOULD_CONFIRM : FoodsharingPickupAutomationDecision.CONFIRMED, reason, userMessage, null);
                    updateLiveCache(livePickupsCache, cacheKey, p.date(), u.id(), true);
                }
                else {
                    log.info("Foodsharing automation decision=decline dryRun={} admin={} foodsharingUserId={} email={} storeId={} pickupDate={} userId={} userName={}", dryRun, a.getAdminConnection().getAdminUser().getId(), a.getAdminConnection().getFoodsharingUserId(), a.getAdminConnection().getFoodsharingEmail(), a.getStoreId(), p.date(), u.id(), u.name());
                    if (!dryRun) client.decline(a.getAdminConnection(), a.getStoreId(), p.date(), u.id(), reason);
                    declined++;
                    saveAudit(a, u.id(), u.name(), p.date(), dryRun, dryRun ? FoodsharingPickupAutomationDecision.WOULD_DECLINE : FoodsharingPickupAutomationDecision.DECLINED, reason, userMessage, null);
                    updateLiveCache(livePickupsCache, cacheKey, p.date(), u.id(), false);
                }
            } catch (RuntimeException ex) { failed++; messages.add(ex.getMessage()); saveAudit(a, u.id(), u.name(), p.date(), dryRun, FoodsharingPickupAutomationDecision.FAILED, "Automation failed", null, ex.getMessage()); }
            }
        }
        log.info("Foodsharing automation run finished (evaluated={}, confirmed={}, declined={}, failed={}, dryRun={}, connections={})", evaluated, confirmed, declined, failed, dryRun, connectionSummary);
        return new FoodsharingPickupModels.RunResult(evaluated, confirmed, declined, failed, dryRun, messages);
    }

    private FoodsharingPickupModels.Decision evaluate(FoodsharingStoreAutomation a, Instant pickupDate, String userId, Map<String, List<FoodsharingPickupModels.Pickup>> livePickupsCache, Map<String, List<FoodsharingPickupModels.Pickup>> initialPickupsCache) {
        List<String> reasons = new ArrayList<>();
        if (a.isGapRuleEnabled()) {
                pickups(livePickupsCache, a.getAdminConnection(), a.getStoreId()).stream()
                    .filter(p -> !p.date().equals(pickupDate))
                    .filter(p -> p.users().stream().anyMatch(u -> u.id().equals(userId)))
                    .map(FoodsharingPickupModels.Pickup::date)
                    .map(d -> calendarGapDays(d, pickupDate))
                    .filter(gapDays -> gapDays < a.getMinimumGapDays())
                    .findAny()
                    .ifPresent(gapDays -> reasons.add("Zwischen den Abholungen liegt nur " + gapDays + daySuffix(gapDays) + " Abstand; erforderlich sind mindestens " + a.getMinimumGapDays() + daySuffix(a.getMinimumGapDays()) + "."));
        }
        if (a.isExperienceRuleEnabled()) {
            List<FoodsharingPickupModels.StoreMember> storeMembers = storeMembers(a.getAdminConnection(), a.getStoreId());
            if (!hasStoreExperience(storeMembers, userId) && !hasExperiencedCoPickup(a, pickupDate, userId, livePickupsCache, storeMembers)) {
                reasons.add("Diese Abholung benötigt Erfahrung in diesem Betrieb. Weder du noch die eventuelle mitabholende Personen haben diese.");
            }
        }
        if (a.isCleaningRuleEnabled()) {
            long cleaningStoreId = appProperties.getFoodsharing().getAutomation().getCleaningStoreId();
            if (cleaningStoreId > 0) {
                Instant now = Instant.now();
                long backCheckMonths = Math.max(0, appProperties.getFoodsharing().getAutomation().getCleaningBackCheckMonths());
                Instant cleaningThreshold = now.atZone(SWISS_ZONE).minusMonths(backCheckMonths).toInstant();
                List<FoodsharingPickupModels.StoreMember> cleaningMembers = storeMembers(a.getAdminConnection(), cleaningStoreId);
                Instant lastCleaning = cleaningMembers.stream()
                        .filter(member -> String.valueOf(member.id()).equals(userId))
                        .map(FoodsharingPickupModels.StoreMember::lastFetch)
                        .filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
                boolean past = lastCleaning != null && !lastCleaning.isBefore(cleaningThreshold);
                List<FoodsharingPickupModels.Pickup> cleaningPickups = pickups(initialPickupsCache, a.getAdminConnection(), cleaningStoreId);
                boolean future = cleaningPickups.stream().filter(p -> p.users().stream().anyMatch(u -> u.id().equals(userId))).anyMatch(p -> !p.date().isBefore(now) && !p.date().isAfter(now.plus(Duration.ofDays(14))));
                if (!past && !future) {
                    String historyText;
                    if (lastCleaning == null) {
                        historyText = "Du hast bisher noch keinen Fairteiler gereinigt.";
                    } else {
                        long lastCleaningMonths = monthsAgo(lastCleaning, now);
                        historyText = "Deine letzte Reinigung ist " + lastCleaningMonths + monthSuffix(lastCleaningMonths) + " her.";
                    }
                    reasons.add("Du hast in den letzten " + backCheckMonths + monthSuffix(backCheckMonths) + " keinen Fairteiler geputzt. " + historyText + " Zudem hast du in den nächsten zwei Wochen keine Reinigung geplant. Bitte trage dich für eine Reinigung ein, damit weitere Abholungen geplant werden können.");
                }
            }
        }
        if (reasons.isEmpty()) reasons.add("All enabled pickup automation rules passed.");
        return new FoodsharingPickupModels.Decision(reasons.size() == 1 && reasons.get(0).startsWith("All enabled"), reasons);
    }

    private List<FoodsharingPickupModels.Pickup> storePickups(FoodsharingAdminConnection connection, long storeId) {
        Duration ttl = Duration.parse(appProperties.getFoodsharing().getAutomation().getStorePickupCacheTtl());
        Instant now = Instant.now();
        var existingCache = storePickupsCacheRepository.findByAdminConnectionAndStoreId(connection, storeId);
        if (existingCache.isPresent()) {
            FoodsharingStorePickupsCache cache = existingCache.get();
            if (cache.getRefreshedAt() != null && cache.getRefreshedAt().plus(ttl).isAfter(now)) {
                try {
                    return deserializeStorePickups(cache.getPayloadJson());
                } catch (RuntimeException ignored) {
                    // Fall through and refresh from Foodsharing if the cached payload is unreadable.
                }
            }
        }
        List<FoodsharingPickupModels.Pickup> fresh = client.pickups(connection, storeId);
        FoodsharingStorePickupsCache cache = existingCache.orElseGet(FoodsharingStorePickupsCache::new);
        cache.setAdminConnection(connection);
        cache.setStoreId(storeId);
        cache.setRefreshedAt(now);
        cache.setPayloadJson(serializeStorePickups(fresh));
        storePickupsCacheRepository.save(cache);
        return fresh;
    }

    private List<FoodsharingPickupModels.Pickup> pickups(Map<String, List<FoodsharingPickupModels.Pickup>> cache, FoodsharingAdminConnection connection, long storeId) {
        return cache.computeIfAbsent(cacheKey(connection, storeId), key -> storePickups(connection, storeId));
    }

    private String cacheKey(FoodsharingAdminConnection connection, long storeId) {
        return connection.getId() + ":" + storeId;
    }

    private List<FoodsharingPickupModels.Pickup> copyPickups(List<FoodsharingPickupModels.Pickup> pickups) {
        return pickups.stream()
                .map(pickup -> new FoodsharingPickupModels.Pickup(pickup.storeId(), pickup.date(), new ArrayList<>(pickup.users())))
                .toList();
    }

    private void updateLiveCache(Map<String, List<FoodsharingPickupModels.Pickup>> cache, String cacheKey, Instant pickupDate, String userId, boolean confirmed) {
        List<FoodsharingPickupModels.Pickup> current = cache.get(cacheKey);
        if (current == null) {
            return;
        }
        List<FoodsharingPickupModels.Pickup> updated = new ArrayList<>();
        for (FoodsharingPickupModels.Pickup pickup : current) {
            if (!pickup.date().equals(pickupDate)) {
                updated.add(pickup);
                continue;
            }
            List<FoodsharingPickupModels.PickupUser> users = new ArrayList<>(pickup.users());
            if (confirmed) {
                for (int i = 0; i < users.size(); i++) {
                    FoodsharingPickupModels.PickupUser currentUser = users.get(i);
                    if (currentUser.id().equals(userId)) {
                        users.set(i, new FoodsharingPickupModels.PickupUser(currentUser.id(), currentUser.name(), true));
                        break;
                    }
                }
            } else {
                users.removeIf(currentUser -> currentUser.id().equals(userId));
            }
            updated.add(new FoodsharingPickupModels.Pickup(pickup.storeId(), pickup.date(), users));
        }
        cache.put(cacheKey, updated);
    }

    private boolean hasExperiencedCoPickup(FoodsharingStoreAutomation automation, Instant pickupDate, String userId, Map<String, List<FoodsharingPickupModels.Pickup>> livePickupsCache, List<FoodsharingPickupModels.StoreMember> storeMembers) {
        return pickups(livePickupsCache, automation.getAdminConnection(), automation.getStoreId()).stream()
                .filter(pickup -> pickup.date().equals(pickupDate))
                .flatMap(pickup -> pickup.users().stream())
                .filter(pickupUser -> !pickupUser.id().equals(userId))
                .anyMatch(pickupUser -> hasStoreExperience(storeMembers, pickupUser.id()));
    }

    private boolean hasStoreExperience(List<FoodsharingPickupModels.StoreMember> storeMembers, String userId) {
        return storeMembers.stream()
                .filter(member -> String.valueOf(member.id()).equals(userId))
                .anyMatch(member -> member.fetchCount() > 0 || member.lastFetch() != null);
    }

    private long calendarGapDays(Instant left, Instant right) {
        LocalDate leftDate = left.atZone(SWISS_ZONE).toLocalDate();
        LocalDate rightDate = right.atZone(SWISS_ZONE).toLocalDate();
        return Math.max(0, Math.abs(ChronoUnit.DAYS.between(leftDate, rightDate)) - 1);
    }

    private void saveAudit(FoodsharingStoreAutomation a, String userId, String userName, Instant pickupDate, boolean dryRun, FoodsharingPickupAutomationDecision decision, String reasons, String userMessage, String error) { var audit = new FoodsharingPickupAutomationAudit(); audit.setAdminConnection(a.getAdminConnection()); audit.setStoreId(a.getStoreId()); audit.setFoodsharingUserId(userId); audit.setFoodsharingUserName(userName == null || userName.isBlank() ? null : userName); audit.setPickupDate(pickupDate); audit.setDryRun(dryRun); audit.setDecision(decision); audit.setReasons(reasons); audit.setUserMessage(userMessage); audit.setFoodsharingError(error); auditRepository.save(audit); }
    private FoodsharingAdminConnection requireConnection() { return connectionRepository.findByAdminUser(currentActorService.requireTeacher()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED)); }
    private StoreAutomationView view(FoodsharingStoreAutomation a) { return new StoreAutomationView(a.getStoreId(), a.getStoreName(), a.isEnabled(), a.isGapRuleEnabled(), a.getMinimumGapDays(), a.isCleaningRuleEnabled(), a.isExperienceRuleEnabled()); }
    private AuditView view(FoodsharingPickupAutomationAudit a, Map<Long, String> storeNames) { return new AuditView(a.getStoreId(), storeNames.getOrDefault(a.getStoreId(), String.valueOf(a.getStoreId())), a.getFoodsharingUserId(), a.getFoodsharingUserName(), a.getPickupDate(), a.isDryRun(), a.getDecision().name(), a.getReasons(), a.getUserMessage(), a.getFoodsharingError(), a.getCreatedAt()); }
    private long monthsAgo(Instant value, Instant now) { return value == null ? 0 : ChronoUnit.MONTHS.between(value.atZone(SWISS_ZONE).toLocalDate(), now.atZone(SWISS_ZONE).toLocalDate()); }
    private String monthSuffix(long months) { return months == 1 ? " Monat" : " Monaten"; }
    private String daySuffix(long days) { return days == 1 ? " Tag" : " Tage"; }

    public record ConnectionStatus(boolean connected, String email, String foodsharingUserId, Instant authenticatedAt) {}
    public record StoreAutomationRequest(String storeName, boolean enabled, boolean gapRuleEnabled, int minimumGapDays, boolean cleaningRuleEnabled, boolean experienceRuleEnabled) {}
    public record StoreAutomationView(long storeId, String storeName, boolean enabled, boolean gapRuleEnabled, int minimumGapDays, boolean cleaningRuleEnabled, boolean experienceRuleEnabled) {}
    public record AuditView(long storeId, String storeName, String foodsharingUserId, String foodsharingUserName, Instant pickupDate, boolean dryRun, String decision, String reasons, String userMessage, String error, Instant createdAt) {}
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
