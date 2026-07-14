package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingCleaningRuleExemption;
import ch.it4user.foodsharing.domain.entity.FoodsharingFuturePickupUsersCache;
import ch.it4user.foodsharing.domain.entity.FoodsharingPickupAutomationAudit;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreMembersCache;
import ch.it4user.foodsharing.domain.entity.FoodsharingStorePickupsCache;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomationAudit;
import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAudit;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.FoodsharingPickupAutomationDecision;
import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.repository.FoodsharingAdminConnectionRepository;
import ch.it4user.foodsharing.repository.FoodsharingCleaningRuleExemptionRepository;
import ch.it4user.foodsharing.repository.FoodsharingPickupAutomationAuditRepository;
import ch.it4user.foodsharing.repository.FoodsharingFuturePickupUsersCacheRepository;
import ch.it4user.foodsharing.repository.FoodsharingStoreMembersCacheRepository;
import ch.it4user.foodsharing.repository.FoodsharingStorePickupsCacheRepository;
import ch.it4user.foodsharing.repository.FoodsharingStoreAutomationRepository;
import ch.it4user.foodsharing.repository.FoodsharingRequestAutomationRepository;
import ch.it4user.foodsharing.repository.FoodsharingRequestAutomationAuditRepository;
import ch.it4user.foodsharing.repository.FoodsharingOpenSlotAdvertisementAutomationRepository;
import ch.it4user.foodsharing.repository.FoodsharingOpenSlotAdvertisementAuditRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodsharingPickupAutomationService {
    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final Logger log = LoggerFactory.getLogger(FoodsharingPickupAutomationService.class);
    private final AppProperties appProperties;
    private final BezirkService bezirkService;
    private final CurrentActorService currentActorService;
    private final CryptoService cryptoService;
    private final FoodsharingPickupApiClient client;
    private final FoodsharingAdminConnectionRepository connectionRepository;
    private final FoodsharingStoreAutomationRepository automationRepository;
    private final FoodsharingRequestAutomationRepository requestAutomationRepository;
    private final FoodsharingRequestAutomationAuditRepository requestAutomationAuditRepository;
    private final FoodsharingOpenSlotAdvertisementAutomationRepository advertisementAutomationRepository;
    private final FoodsharingOpenSlotAdvertisementAuditRepository advertisementAuditRepository;
    private final FoodsharingPickupAutomationAuditRepository auditRepository;
    private final FoodsharingFuturePickupUsersCacheRepository futurePickupUsersCacheRepository;
    private final FoodsharingStoreMembersCacheRepository storeMembersCacheRepository;
    private final FoodsharingStorePickupsCacheRepository storePickupsCacheRepository;
    private final FoodsharingCleaningRuleExemptionRepository cleaningRuleExemptionRepository;
    private final FoodsharingMessageService messageService;
    private final ObjectMapper objectMapper;
    private final RestClient telegramClient = RestClient.create("https://api.telegram.org");
    private final AtomicBoolean scheduledRunInProgress = new AtomicBoolean(false);

    public FoodsharingPickupAutomationService(AppProperties appProperties, BezirkService bezirkService, CurrentActorService currentActorService, CryptoService cryptoService, FoodsharingPickupApiClient client, FoodsharingAdminConnectionRepository connectionRepository, FoodsharingStoreAutomationRepository automationRepository, FoodsharingRequestAutomationRepository requestAutomationRepository, FoodsharingRequestAutomationAuditRepository requestAutomationAuditRepository, FoodsharingOpenSlotAdvertisementAutomationRepository advertisementAutomationRepository, FoodsharingOpenSlotAdvertisementAuditRepository advertisementAuditRepository, FoodsharingPickupAutomationAuditRepository auditRepository, FoodsharingFuturePickupUsersCacheRepository futurePickupUsersCacheRepository, FoodsharingStoreMembersCacheRepository storeMembersCacheRepository, FoodsharingStorePickupsCacheRepository storePickupsCacheRepository, FoodsharingCleaningRuleExemptionRepository cleaningRuleExemptionRepository, FoodsharingMessageService messageService, ObjectMapper objectMapper) {
        this.appProperties = appProperties; this.bezirkService = bezirkService; this.currentActorService = currentActorService; this.cryptoService = cryptoService; this.client = client; this.connectionRepository = connectionRepository; this.automationRepository = automationRepository; this.requestAutomationRepository = requestAutomationRepository; this.requestAutomationAuditRepository = requestAutomationAuditRepository; this.advertisementAutomationRepository = advertisementAutomationRepository; this.advertisementAuditRepository = advertisementAuditRepository; this.auditRepository = auditRepository; this.futurePickupUsersCacheRepository = futurePickupUsersCacheRepository; this.storeMembersCacheRepository = storeMembersCacheRepository; this.storePickupsCacheRepository = storePickupsCacheRepository; this.cleaningRuleExemptionRepository = cleaningRuleExemptionRepository; this.messageService = messageService; this.objectMapper = objectMapper;
    }

    @Transactional
    public ConnectionStatus connect(String email, String password, String telegramBotToken) {
        User admin = currentActorService.requireAutomationUser();
        var session = client.login(email, password);
        FoodsharingAdminConnection c = connectionRepository.findByAdminUser(admin).orElseGet(FoodsharingAdminConnection::new);
        c.setAdminUser(admin); c.setFoodsharingEmail(email); c.setFoodsharingPasswordCiphertext(cryptoService.encrypt(password)); c.setFoodsharingUserId(session.foodsharingUserId()); c.setSessionCookieCiphertext(cryptoService.encrypt(session.cookie())); c.setCsrfTokenCiphertext(cryptoService.encrypt(session.csrf())); c.setTelegramBotTokenCiphertext(telegramBotToken == null || telegramBotToken.isBlank() ? null : cryptoService.encrypt(telegramBotToken.trim())); c.setAuthenticatedAt(Instant.now());
        connectionRepository.save(c);
        log.info("Foodsharing automation connection saved admin={} foodsharingUserId={} email={}", admin.getId(), session.foodsharingUserId(), email);
        return status(c);
    }

    @Transactional
    public void saveTelegramBotToken(String telegramBotToken) {
        FoodsharingAdminConnection connection = requireConnection();
        connection.setTelegramBotTokenCiphertext(telegramBotToken == null || telegramBotToken.isBlank() ? null : cryptoService.encrypt(telegramBotToken.trim()));
        connectionRepository.save(connection);
    }

    @Transactional
    public void disconnect() {
        User admin = currentActorService.requireAutomationUser();
        connectionRepository.findByAdminUser(admin).ifPresent(connection -> {
            log.info("Foodsharing automation connection removed admin={} foodsharingUserId={} email={}", admin.getId(), connection.getFoodsharingUserId(), connection.getFoodsharingEmail());
            auditRepository.deleteAllByAdminConnection(connection);
            futurePickupUsersCacheRepository.deleteAllByAdminConnection(connection);
            storeMembersCacheRepository.deleteAllByAdminConnection(connection);
            storePickupsCacheRepository.deleteAllByAdminConnection(connection);
            automationRepository.deleteAllByAdminConnection(connection);
            requestAutomationRepository.deleteAllByAdminConnection(connection);
            advertisementAutomationRepository.deleteAllByAdminConnection(connection);
            connectionRepository.delete(connection);
        });
    }
    public ConnectionStatus status() { return connectionRepository.findByAdminUser(currentActorService.requireAutomationUser()).map(this::status).orElse(new ConnectionStatus(false, null, null, null, false, appProperties.getFoodsharing().getAutomation().isEnabled(), appProperties.getFoodsharing().getAutomation().isDryRun())); }
    private ConnectionStatus status(FoodsharingAdminConnection c) { return new ConnectionStatus(true, c.getFoodsharingEmail(), c.getFoodsharingUserId(), c.getAuthenticatedAt(), c.getTelegramBotTokenCiphertext() != null && !c.getTelegramBotTokenCiphertext().isBlank(), appProperties.getFoodsharing().getAutomation().isEnabled(), appProperties.getFoodsharing().getAutomation().isDryRun()); }

    @Transactional
    public StoreOverviewView stores() {
        FoodsharingAdminConnection c = requireConnection();
        return stores(c, requireBezirk(c.getAdminUser()));
    }

    @Transactional
    public StoreOverviewView stores(String bezirkSlug) {
        FoodsharingAdminConnection c = requireConnection();
        return stores(c, requireBezirk(bezirkSlug, c.getAdminUser()));
    }

    private StoreOverviewView stores(FoodsharingAdminConnection c, Bezirk bezirk) {
        List<FoodsharingPickupModels.Store> managedStores = managedStores(c);
        Map<Long, FoodsharingPickupModels.Store> managedStoresById = managedStores.stream()
                .collect(java.util.stream.Collectors.toMap(FoodsharingPickupModels.Store::id, store -> store, (left, right) -> left, LinkedHashMap::new));
        List<StoreAutomationView> automations = automationRepository.findAllByBezirk(bezirk).stream()
                .sorted(Comparator.comparing(FoodsharingStoreAutomation::getStoreName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparingLong(FoodsharingStoreAutomation::getStoreId))
                .map(automation -> view(
                        automation.getStoreId(),
                        managedStoresById.getOrDefault(automation.getStoreId(), new FoodsharingPickupModels.Store(automation.getStoreId(), automation.getStoreName(), false)).name(),
                        automation,
                        c))
                .toList();
        List<ManagedStoreView> availableStores = managedStores.stream()
                .filter(store -> automations.stream().noneMatch(automation -> automation.storeId() == store.id()))
                .map(store -> new ManagedStoreView(store.id(), store.name()))
                .sorted(Comparator.comparing(ManagedStoreView::storeName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparingLong(ManagedStoreView::storeId))
                .toList();
        return new StoreOverviewView(automations, availableStores);
    }

    @Transactional
    public StoreAutomationView save(long storeId, StoreAutomationRequest request) {
        FoodsharingAdminConnection c = requireConnection();
        return save(c, requireBezirk(c.getAdminUser()), storeId, request);
    }

    @Transactional
    public StoreAutomationView save(String bezirkSlug, long storeId, StoreAutomationRequest request) {
        FoodsharingAdminConnection c = requireConnection();
        return save(c, requireBezirk(bezirkSlug, c.getAdminUser()), storeId, request);
    }

    private StoreAutomationView save(FoodsharingAdminConnection c, Bezirk bezirk, long storeId, StoreAutomationRequest request) {
        FoodsharingStoreAutomation a = automationRepository.findByStoreId(storeId)
                .map(existing -> {
                    if (!existing.getBezirk().getId().equals(bezirk.getId())
                            || !existing.getAdminConnection().getId().equals(c.getId())) {
                        throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.VALIDATION_FAILED, List.of("An automation already exists for this store."));
                    }
                    return existing;
                })
                .orElseGet(() -> { var n = new FoodsharingStoreAutomation(); n.setBezirk(bezirk); n.setAdminConnection(c); n.setStoreId(storeId); return n; });
        a.setStoreName(request.storeName() == null ? a.getStoreName() : request.storeName()); a.setEnabled(request.enabled()); a.setDryRunEnabled(request.dryRunEnabled()); a.setGapRuleEnabled(request.gapRuleEnabled()); a.setMinimumGapDays(Math.max(0, request.minimumGapDays())); a.setCleaningRuleEnabled(request.cleaningRuleEnabled() && bezirk.getCleaningStoreId() != null); a.setExperienceRuleEnabled(request.experienceRuleEnabled());
        FoodsharingStoreAutomation saved = automationRepository.save(a);
        futurePickupUsersCacheRepository.deleteByAdminConnectionAndBezirk(c, bezirk);
        return view(storeId, saved.getStoreName(), saved, c);
    }

    @Transactional
    public void delete(long storeId) {
        FoodsharingAdminConnection connection = requireConnection();
        delete(connection, requireBezirk(connection.getAdminUser()), storeId);
    }

    @Transactional
    public void delete(String bezirkSlug, long storeId) {
        FoodsharingAdminConnection connection = requireConnection();
        delete(connection, requireBezirk(bezirkSlug, connection.getAdminUser()), storeId);
    }

    private void delete(FoodsharingAdminConnection connection, Bezirk bezirk, long storeId) {
        FoodsharingStoreAutomation automation = automationRepository.findByBezirkAndStoreId(bezirk, storeId)
                .filter(entry -> entry.getAdminConnection().getId().equals(connection.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, List.of("Store automation not found.")));
        automationRepository.delete(automation);
        futurePickupUsersCacheRepository.deleteByAdminConnectionAndBezirk(connection, bezirk);
    }



    @Transactional(readOnly = true)
    public List<RequestAutomationView> requestAutomationOverview() {
        FoodsharingAdminConnection connection = requireConnection();
        return requestAutomationRepository.findAll().stream()
                .filter(a -> a.getAdminConnection().getId().equals(connection.getId()))
                .map(a -> new RequestAutomationView(a.getStoreId(), a.getStoreName(), a.isEnabled(), a.isDryRunEnabled(), a.isDistanceRuleEnabled(), a.getMaximumDistanceKm(), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdvertisementAutomationView> openSlotAdvertisementOverview() {
        FoodsharingAdminConnection connection = requireConnection();
        return advertisementAutomationRepository.findAll().stream()
                .filter(a -> a.getAdminConnection().getId().equals(connection.getId()))
                .map(a -> advertisementView(a, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExtraAutomationOverviewView extraAutomationOverview() {
        return new ExtraAutomationOverviewView(requestAutomationOverview(), openSlotAdvertisementOverview());
    }

    @Transactional
    public RequestAutomationView saveRequestAutomation(long storeId, RequestAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        FoodsharingAdminConnection c = requireConnection();
        FoodsharingRequestAutomation a = requestAutomationRepository.findByStoreId(storeId)
                .map(existing -> {
                    if (!existing.getAdminConnection().getId().equals(c.getId())) throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.VALIDATION_FAILED, List.of("A request automation already exists for this store."));
                    return existing;
                })
                .orElseGet(() -> { var n = new FoodsharingRequestAutomation(); n.setAdminConnection(c); n.setStoreId(storeId); return n; });
        validateRequestAutomation(request);
        a.setStoreName(request.storeName() == null ? a.getStoreName() : request.storeName());
        a.setEnabled(request.enabled());
        a.setDryRunEnabled(request.dryRunEnabled());
        a.setDistanceRuleEnabled(request.distanceRuleEnabled());
        a.setMaximumDistanceKm(normalizeDistance(request.maximumDistanceKm()));
        a = requestAutomationRepository.save(a);
        return new RequestAutomationView(a.getStoreId(), a.getStoreName(), a.isEnabled(), a.isDryRunEnabled(), a.isDistanceRuleEnabled(), a.getMaximumDistanceKm(), a.getAdminConnection().getId().equals(c.getId()));
    }

    @Transactional
    public void deleteRequestAutomation(long storeId) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        FoodsharingAdminConnection connection = requireConnection();
        FoodsharingRequestAutomation automation = requestAutomationRepository.findByStoreId(storeId)
                .filter(entry -> entry.getAdminConnection().getId().equals(connection.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, List.of("Request automation not found.")));
        requestAutomationAuditRepository.deleteAllByAutomation(automation);
        requestAutomationRepository.delete(automation);
    }

    @Transactional
    public AdvertisementAutomationView saveAdvertisementAutomation(long storeId, int advertNumber, AdvertisementAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        if (advertNumber < 1 || advertNumber > 3) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Advert number must be between 1 and 3."));
        FoodsharingAdminConnection c = requireConnection();
        FoodsharingOpenSlotAdvertisementAutomation a = advertisementAutomationRepository.findByStoreIdAndAdvertNumber(storeId, advertNumber)
                .orElseGet(() -> { var n = new FoodsharingOpenSlotAdvertisementAutomation(); n.setAdminConnection(c); n.setStoreId(storeId); n.setAdvertNumber(advertNumber); return n; });
        if (!a.getAdminConnection().getId().equals(c.getId())) throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.VALIDATION_FAILED, List.of("An advertisement automation already exists for this store."));
        a.setStoreName(request.storeName() == null ? a.getStoreName() : request.storeName());
        a.setEnabled(request.enabled()); a.setTriggerHoursBefore(Math.max(1, request.triggerHoursBefore()));
        a.setSendToStoreChat(request.sendToStoreChat()); a.setSendToTelegram(request.sendToTelegram()); a.setTelegramChatId(blankToNull(request.telegramChatId()));
        validateTemplateVariables(request.messages());
        a.setMessagesJson(serializeMessages(request.messages()));
        a = advertisementAutomationRepository.save(a);
        return advertisementView(a, true);
    }

    @Transactional
    public void deleteAdvertisementAutomation(long storeId, int advertNumber) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        if (advertNumber < 1 || advertNumber > 3) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Advert number must be between 1 and 3."));
        FoodsharingAdminConnection connection = requireConnection();
        FoodsharingOpenSlotAdvertisementAutomation automation = advertisementAutomationRepository.findByStoreIdAndAdvertNumber(storeId, advertNumber)
                .filter(entry -> entry.getAdminConnection().getId().equals(connection.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, List.of("Advertisement automation not found.")));
        advertisementAuditRepository.deleteAllByAutomation(automation);
        advertisementAutomationRepository.delete(automation);
    }

    @Transactional
    public AutomationRunSummary runRequestAutomations() {
        return runRequestAutomations(false);
    }

    @Transactional
    public AutomationRunSummary runRequestAutomations(boolean dryRunOverride) {
        if (!appProperties.getFoodsharing().getAutomation().isEnabled()) {
            return new AutomationRunSummary(0, 0, 1, true, List.of("Request automation skipped: global automation is disabled."));
        }
        boolean globalDryRun = dryRunOverride || appProperties.getFoodsharing().getAutomation().isDryRun();
        int evaluated = 0;
        int acted = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        int maxApprovals = Math.max(0, appProperties.getFoodsharing().getAutomation().getMaxRequestApprovalsPerRun());
        for (FoodsharingRequestAutomation a : requestAutomationRepository.findAllByEnabledTrue()) {
            boolean dryRun = globalDryRun || a.isDryRunEnabled();
            List<FoodsharingPickupApiClient.RequestUser> requests = client.requests(a.getAdminConnection(), a.getStoreId());
            if (requests.isEmpty()) {
                skipped++;
                String message = "No open requests for " + a.getStoreName() + " (" + a.getStoreId() + ").";
                saveRequestAudit(a, (String) null, (String) null, dryRun, "SKIPPED", message, null, null);
                messages.add(message);
            }
            for (FoodsharingPickupApiClient.RequestUser user : requests) {
                if (acted >= maxApprovals) {
                    skipped++;
                    String message = "Request automation limit reached (" + maxApprovals + ").";
                    saveRequestAudit(a, user, dryRun, "SKIPPED", message, null, null);
                    messages.add(message);
                    break;
                }
                evaluated++;
                log.info("Foodsharing request automation approving dryRun={} storeId={} userId={} userName={}", dryRun, a.getStoreId(), user.id(), user.name());
                if (user.id() == null || user.id().isBlank()) { skipped++; saveRequestAudit(a, user, dryRun, "SKIPPED", "Missing Foodsharing user id.", null, null); continue; }
                if (a.isDistanceRuleEnabled()) {
                    Double distanceInKm = user.distanceInKm();
                    if (distanceInKm == null) {
                        skipped++;
                        saveRequestAudit(a, user, dryRun, "SKIPPED", "Missing request distance.", null, null);
                        continue;
                    }
                    if (distanceInKm > a.getMaximumDistanceKm()) {
                        String message = "Die Anfrage wurde automatisch abgelehnt, weil die Entfernung von " + formatDistance(distanceInKm) + " km das Maximum von " + formatDistance(a.getMaximumDistanceKm()) + " km überschreitet.";
                        if (!dryRun) {
                            client.declineRequest(a.getAdminConnection(), a.getStoreId(), user.id(), message);
                            invalidateStoreCaches(a.getAdminConnection(), a.getStoreId());
                            acted++;
                            saveRequestAudit(a, user, dryRun, "DECLINED", message, message, null);
                        } else {
                            skipped++;
                            saveRequestAudit(a, user, dryRun, "WOULD_DECLINE", message, message, null);
                        }
                        messages.add((dryRun ? "Würde ablehnen: " : "Abgelehnt: ") + message);
                        continue;
                    }
                }
                try {
                    if (!dryRun) { client.approveRequest(a.getAdminConnection(), a.getStoreId(), user.id()); invalidateStoreCaches(a.getAdminConnection(), a.getStoreId()); }
                    acted++;
                    String reason = (dryRun ? "Would approve request." : "Request approved.");
                    saveRequestAudit(a, user, dryRun, dryRun ? "WOULD_APPROVE" : "APPROVED", reason, null, null);
                    messages.add((dryRun ? "Would approve " : "Approved ") + user.name() + " (" + user.id() + ") for " + a.getStoreName() + ".");
                } catch (RuntimeException ex) {
                    skipped++;
                    saveRequestAudit(a, user, dryRun, "FAILED", "Request approval failed.", null, ex.getMessage());
                    messages.add("Failed to approve " + user.name() + " (" + user.id() + "): " + ex.getMessage());
                }
            }
        }
        return new AutomationRunSummary(evaluated, acted, skipped, globalDryRun, messages);
    }

    @Scheduled(fixedDelayString = "#{T(java.time.Duration).parse('${app.foodsharing.automation.poll-interval:PT5M}').toMillis()}")
    @Transactional
    public void scheduledAutomationRun() {
        if (!scheduledRunInProgress.compareAndSet(false, true)) {
            log.info("Foodsharing automation poll skipped because another run is still in progress.");
            return;
        }
        try {
            runRequestAutomations();
            scheduledRun();
            runAdvertisementAutomations(false);
        } finally {
            scheduledRunInProgress.set(false);
        }
    }

    @Transactional
    public AutomationRunSummary runAdvertisementAutomations() {
        return runAdvertisementAutomations(false);
    }

    @Transactional
    public AutomationRunSummary runAdvertisementAutomations(boolean dryRunOverride) {
        if (!appProperties.getFoodsharing().getAutomation().isEnabled()) {
            return new AutomationRunSummary(0, 0, 1, true, List.of("Open-slot advertisement automation skipped: global automation is disabled."));
        }
        boolean dryRun = dryRunOverride || appProperties.getFoodsharing().getAutomation().isDryRun();
        Instant now = Instant.now();
        int evaluated = 0;
        int acted = 0;
        int skipped = 0;
        int maxAdvertisements = Math.max(0, appProperties.getFoodsharing().getAutomation().getMaxAdvertisementsPerRun());
        for (FoodsharingOpenSlotAdvertisementAutomation a : advertisementAutomationRepository.findAllByEnabledTrue()) {
            List<String> messages = deserializeMessages(a.getMessagesJson());
            int checkedSlots = 0;
            int automationActed = 0;
            if (messages.isEmpty()) {
                skipped++;
                if (dryRun) {
                    saveAdvertisementRunSummaryAudit(a, now, true, 0, 0, 0, "Advertisement skipped: no messages configured.");
                }
                continue;
            }
            if (!a.isSendToStoreChat() && !a.isSendToTelegram()) {
                skipped++;
                if (dryRun) {
                    saveAdvertisementRunSummaryAudit(a, now, true, 0, 0, 0, "Advertisement skipped: no output target selected.");
                }
                continue;
            }
            for (FoodsharingPickupModels.Pickup p : storePickups(a.getAdminConnection(), a.getStoreId())) {
                evaluated++;
                checkedSlots++;
                if (!p.users().isEmpty()) {
                    if (!dryRun) {
                        deleteTelegramAdvertisementsForFilledSlot(a, p.date());
                    }
                    skipped++;
                    continue;
                }
                Instant triggerAt = p.date().minus(Duration.ofHours(a.getTriggerHoursBefore()));
                if (triggerAt.isAfter(now) || now.isAfter(p.date())) {
                    skipped++;
                    continue;
                }
                if (advertisementAuditRepository.existsByAutomationAndPickupDateAndTriggerHoursBefore(a, p.date(), a.getTriggerHoursBefore())) {
                    skipped++;
                    continue;
                }
                if (acted >= maxAdvertisements) {
                    skipped++;
                    break;
                }
                String message = renderAdvertisement(messages.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(messages.size())), p.date(), a.getAdminConnection().getFoodsharingUserId());
                if (!dryRun && a.isSendToStoreChat()) client.sendStoreChatMessage(a.getAdminConnection(), a.getStoreId(), message);
                Integer telegramMessageId = null;
                if (!dryRun && a.isSendToTelegram() && a.getTelegramChatId() != null) telegramMessageId = sendTelegram(a, message);
                if (dryRun) {
                    automationActed++;
                } else {
                    saveAdvertisementAudit(a, p.date(), false, "SENT", "Advertisement sent.", message, null, telegramMessageId);
                }
                acted++;
            }
            if (dryRun) {
                saveAdvertisementRunSummaryAudit(a, now, true, checkedSlots, automationActed, Math.max(0, checkedSlots - automationActed), "Checked " + checkedSlots + " slot" + (checkedSlots == 1 ? "" : "s") + ".");
            }
        }
        List<String> runMessages = List.of((dryRun ? "Would send " : "Sent ") + acted + " message" + (acted == 1 ? "" : "s") + " after checking " + evaluated + " slot" + (evaluated == 1 ? "" : "s") + ".");
        return new AutomationRunSummary(evaluated, acted, skipped, dryRun, runMessages);
    }

    public void sendTelegramTestMessage(String chatId, String message) {
        FoodsharingAdminConnection connection = requireConnection();
        String token = telegramToken(connection);
        if (token == null || token.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Telegram bot token is not configured."));
        try {
            telegramClient.post().uri("/bot{token}/sendMessage", token).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", normalizeRequired(chatId), "text", normalize(message).isBlank() ? "Foodsharing automation test message" : normalize(message)))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Telegram test message failed: " + ex.getMessage()));
        }
    }


    private void saveRequestAudit(FoodsharingRequestAutomation automation, FoodsharingPickupApiClient.RequestUser user, boolean dryRun, String status, String reason, String message, String error) {
        saveRequestAudit(automation, user.id(), user.name(), dryRun, status, reason, message, error);
    }

    private void saveRequestAudit(FoodsharingRequestAutomation automation, String foodsharingUserId, String foodsharingUserName, boolean dryRun, String status, String reason, String message, String error) {
        FoodsharingRequestAutomationAudit audit = new FoodsharingRequestAutomationAudit();
        audit.setAutomation(automation);
        audit.setStoreId(automation.getStoreId());
        audit.setStoreName(automation.getStoreName());
        audit.setFoodsharingUserId(foodsharingUserId == null || foodsharingUserId.isBlank() ? "unknown" : foodsharingUserId);
        audit.setFoodsharingUserName(foodsharingUserName);
        audit.setDryRun(dryRun);
        audit.setStatus(status);
        audit.setReason(reason);
        audit.setMessage(message);
        audit.setError(error);
        requestAutomationAuditRepository.save(audit);
    }

    private void saveAdvertisementAudit(FoodsharingOpenSlotAdvertisementAutomation automation, Instant pickupDate, boolean dryRun, String status, String reason, String message, String error, Integer telegramMessageId) {
        FoodsharingOpenSlotAdvertisementAudit audit = new FoodsharingOpenSlotAdvertisementAudit();
        audit.setAutomation(automation);
        audit.setStoreId(automation.getStoreId());
        audit.setStoreName(automation.getStoreName());
        audit.setPickupDate(pickupDate);
        audit.setTriggerHoursBefore(automation.getTriggerHoursBefore());
        audit.setTelegramMessageId(telegramMessageId);
        audit.setStatus(status);
        audit.setDryRun(dryRun);
        audit.setMessage(message);
        audit.setReason(reason);
        audit.setError(error);
        advertisementAuditRepository.save(audit);
    }

    private void saveAdvertisementRunSummaryAudit(FoodsharingOpenSlotAdvertisementAutomation automation, Instant pickupDate, boolean dryRun, int checkedSlots, int messagesCount, int skippedSlots, String reason) {
        FoodsharingOpenSlotAdvertisementAudit audit = new FoodsharingOpenSlotAdvertisementAudit();
        audit.setAutomation(automation);
        audit.setStoreId(automation.getStoreId());
        audit.setStoreName(automation.getStoreName());
        audit.setPickupDate(pickupDate);
        audit.setTriggerHoursBefore(automation.getTriggerHoursBefore());
        audit.setStatus(dryRun ? "WOULD_SUMMARY" : "SUMMARY_SENT");
        audit.setDryRun(dryRun);
        audit.setReason(reason + " Checked " + checkedSlots + " slot" + (checkedSlots == 1 ? "" : "s") + ", would send " + messagesCount + " message" + (messagesCount == 1 ? "" : "s") + ", skipped " + skippedSlots + " slot" + (skippedSlots == 1 ? "" : "s") + ".");
        audit.setMessage("Messages that would be sent: " + messagesCount + ".");
        advertisementAuditRepository.save(audit);
    }

    private void validateRequestAutomation(RequestAutomationRequest request) {
        if (request.maximumDistanceKm() != null && request.maximumDistanceKm() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Maximum distance must not be negative."));
        }
        if (request.distanceRuleEnabled() && (request.maximumDistanceKm() == null || request.maximumDistanceKm() <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Maximum distance must be greater than 0 when the distance rule is enabled."));
        }
    }

    private double normalizeDistance(Double value) {
        return value == null ? 0.0 : value;
    }

    private String formatDistance(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private void validateTemplateVariables(List<String> messages) {
        java.util.Set<String> allowed = java.util.Set.of("date", "dateDe", "weekday", "time", "datetime", "datetimeDe", "adminFoodsharingId", "adminProfileUrl");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{\\s*([^}\\s]+)\\s*}} ".trim());
        for (String message : messages == null ? List.<String>of() : messages) {
            java.util.regex.Matcher matcher = pattern.matcher(message == null ? "" : message);
            while (matcher.find()) {
                if (!allowed.contains(matcher.group(1))) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("Unknown template variable: {{" + matcher.group(1) + "}}"));
                }
            }
        }
    }

    private void invalidateStoreCaches(FoodsharingAdminConnection connection, long storeId) {
        storePickupsCacheRepository.deleteByAdminConnectionAndStoreId(connection, storeId);
        storeMembersCacheRepository.deleteByAdminConnectionAndStoreId(connection, storeId);
    }

    private Integer sendTelegram(FoodsharingOpenSlotAdvertisementAutomation automation, String text) {
        String token = telegramToken(automation.getAdminConnection());
        if (token == null || token.isBlank()) return null;
        Object response = telegramClient.post().uri("/bot{token}/sendMessage", token).contentType(MediaType.APPLICATION_JSON).body(Map.of("chat_id", automation.getTelegramChatId(), "text", text)).retrieve().body(Object.class);
        if (response instanceof Map<?, ?> root && root.get("result") instanceof Map<?, ?> result && result.get("message_id") instanceof Number messageId) {
            return messageId.intValue();
        }
        return null;
    }

    private void deleteTelegramAdvertisementsForFilledSlot(FoodsharingOpenSlotAdvertisementAutomation automation, Instant pickupDate) {
        if (!automation.isSendToTelegram() || automation.getTelegramChatId() == null) return;
        String token = telegramToken(automation.getAdminConnection());
        if (token == null || token.isBlank()) return;
        for (FoodsharingOpenSlotAdvertisementAudit audit : advertisementAuditRepository.findAllByAutomationAndPickupDateAndTelegramMessageIdIsNotNullAndTelegramDeletedAtIsNull(automation, pickupDate)) {
            telegramClient.post().uri("/bot{token}/deleteMessage", token).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", automation.getTelegramChatId(), "message_id", audit.getTelegramMessageId()))
                    .retrieve().toBodilessEntity();
            audit.setTelegramDeletedAt(Instant.now());
            advertisementAuditRepository.save(audit);
        }
    }


    public List<TelegramChatView> telegramChats() {
        FoodsharingAdminConnection connection = requireConnection();
        String token = telegramToken(connection);
        if (token == null || token.isBlank()) return List.of();
        Object response = telegramClient.get().uri("/bot{token}/getUpdates", token).retrieve().body(Object.class);
        Map<String, TelegramChatView> chats = new LinkedHashMap<>();
        if (response instanceof Map<?, ?> root && root.get("result") instanceof List<?> updates) {
            for (Object update : updates) {
                if (!(update instanceof Map<?, ?> updateMap)) continue;
                Object message = firstPresent(updateMap, "message", "channel_post", "my_chat_member");
                if (message instanceof Map<?, ?> messageMap) {
                    Object chat = messageMap.get("chat");
                    if (chat instanceof Map<?, ?> chatMap) {
                        String id = String.valueOf(chatMap.get("id"));
                        String title = String.valueOf(firstPresent(chatMap, "title", "username", "first_name"));
                        String type = String.valueOf(firstPresent(chatMap, "type"));
                        if (!id.isBlank()) chats.put(id, new TelegramChatView(id, title == null || title.equals("null") ? id : title, type == null || type.equals("null") ? "" : type));
                    }
                }
            }
        }
        return new ArrayList<>(chats.values());
    }

    private String telegramToken(FoodsharingAdminConnection connection) {
        if (connection.getTelegramBotTokenCiphertext() != null && !connection.getTelegramBotTokenCiphertext().isBlank()) {
            return cryptoService.decrypt(connection.getTelegramBotTokenCiphertext());
        }
        return null;
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) return map.get(key);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<AuditView> audit() {
        User user = currentActorService.requirePermission(UserPermission.CAN_SEE_ALL_AUTOMATION_DECISIONS);
        return audit(requireBezirk(user));
    }

    @Transactional(readOnly = true)
    public List<AuditView> audit(String bezirkSlug) {
        User user = currentActorService.requirePermission(UserPermission.CAN_SEE_ALL_AUTOMATION_DECISIONS);
        return audit(requireBezirk(bezirkSlug, user));
    }

    private List<AuditView> audit(Bezirk bezirk) {
        Map<Long, String> storeNames = automationRepository.findAllByBezirk(bezirk).stream()
                .collect(java.util.stream.Collectors.toMap(FoodsharingStoreAutomation::getStoreId, FoodsharingStoreAutomation::getStoreName, (left, right) -> left));
        return auditRepository.findTop100ByBezirkOrderByCreatedAtDesc(bezirk).stream().map(a -> view(a, storeNames)).toList();
    }

    @Transactional
    public List<StorePickupUserView> futurePickupUsers() {
        FoodsharingAdminConnection connection = requireConnection();
        return futurePickupUsers(connection, requireBezirk(connection.getAdminUser()));
    }

    @Transactional
    public List<StorePickupUserView> futurePickupUsers(String bezirkSlug) {
        FoodsharingAdminConnection connection = requireConnection();
        return futurePickupUsers(connection, requireBezirk(bezirkSlug, connection.getAdminUser()));
    }

    private List<StorePickupUserView> futurePickupUsers(FoodsharingAdminConnection connection, Bezirk bezirk) {
        Duration ttl = Duration.parse(appProperties.getFoodsharing().getAutomation().getFuturePickupCacheTtl());
        Instant now = Instant.now();
        var existingCache = futurePickupUsersCacheRepository.findByAdminConnectionAndBezirk(connection, bezirk);
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
        List<StorePickupUserView> fresh = loadFuturePickupUsers(connection, bezirk);
        FoodsharingFuturePickupUsersCache cache = existingCache.orElseGet(FoodsharingFuturePickupUsersCache::new);
        cache.setAdminConnection(connection);
        cache.setBezirk(bezirk);
        cache.setRefreshedAt(now);
        cache.setPayloadJson(serializeFuturePickupUsers(fresh));
        futurePickupUsersCacheRepository.save(cache);
        return fresh;
    }

    private List<StorePickupUserView> loadFuturePickupUsers(FoodsharingAdminConnection connection, Bezirk bezirk) {
        Map<String, StorePickupUserBuilder> users = new LinkedHashMap<>();
        Instant now = Instant.now();
        Set<Long> bezirkStoreIds = automationRepository.findAllByBezirk(bezirk).stream()
                .map(FoodsharingStoreAutomation::getStoreId)
                .collect(java.util.stream.Collectors.toSet());
        for (FoodsharingPickupModels.Store store : managedStores(connection).stream()
                .filter(store -> bezirkStoreIds.contains(store.id()))
                .toList()) {
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


    @Transactional(readOnly = true)
    public List<CleaningRuleExemptionView> cleaningRuleExemptions() {
        User user = currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return cleaningRuleExemptions(requireBezirk(user));
    }

    @Transactional(readOnly = true)
    public List<CleaningRuleExemptionView> cleaningRuleExemptions(String bezirkSlug) {
        User user = currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return cleaningRuleExemptions(requireBezirk(bezirkSlug, user));
    }

    private List<CleaningRuleExemptionView> cleaningRuleExemptions(Bezirk bezirk) {
        return cleaningRuleExemptionRepository.findAllByBezirkOrderByFoodsharingIdAsc(bezirk).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public CleaningRuleExemptionView saveCleaningRuleExemption(CleaningRuleExemptionRequest request) {
        User user = currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return saveCleaningRuleExemption(requireBezirk(user), request);
    }

    @Transactional
    public CleaningRuleExemptionView saveCleaningRuleExemption(String bezirkSlug, CleaningRuleExemptionRequest request) {
        User user = currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return saveCleaningRuleExemption(requireBezirk(bezirkSlug, user), request);
    }

    private CleaningRuleExemptionView saveCleaningRuleExemption(Bezirk bezirk, CleaningRuleExemptionRequest request) {
        String foodsharingId = normalizeRequired(request.foodsharingId());
        String reason = normalizeRequired(request.reason());
        FoodsharingCleaningRuleExemption exemption = cleaningRuleExemptionRepository.findByBezirkAndFoodsharingId(bezirk, foodsharingId)
                .orElseGet(FoodsharingCleaningRuleExemption::new);
        exemption.setBezirk(bezirk);
        exemption.setFoodsharingId(foodsharingId);
        exemption.setReason(reason);
        return view(cleaningRuleExemptionRepository.save(exemption));
    }

    @Transactional
    public void deleteCleaningRuleExemption(UUID exemptionId) {
        User user = currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        deleteCleaningRuleExemption(requireBezirk(user), exemptionId);
    }

    @Transactional
    public void deleteCleaningRuleExemption(String bezirkSlug, UUID exemptionId) {
        User user = currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        deleteCleaningRuleExemption(requireBezirk(bezirkSlug, user), exemptionId);
    }

    private void deleteCleaningRuleExemption(Bezirk bezirk, UUID exemptionId) {
        FoodsharingCleaningRuleExemption exemption = cleaningRuleExemptionRepository.findByIdAndBezirk(exemptionId, bezirk)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND));
        cleaningRuleExemptionRepository.delete(exemption);
    }

    @Transactional
    public FoodsharingPickupModels.RunResult run(boolean dryRun) {
        User user = currentActorService.requireAutomationSlotApprovalUser();
        return run(requireBezirk(user), dryRun);
    }

    @Transactional
    public FoodsharingPickupModels.RunResult run(String bezirkSlug, boolean dryRun) {
        User user = currentActorService.requireAutomationSlotApprovalUser();
        return run(requireBezirk(bezirkSlug, user), dryRun);
    }

    private FoodsharingPickupModels.RunResult run(Bezirk bezirk, boolean dryRun) {
        return run(automationRepository.findAllByBezirkAndEnabledTrue(bezirk),
                dryRun || appProperties.getFoodsharing().getAutomation().isDryRun());
    }

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

    private FoodsharingPickupModels.RunResult run(List<FoodsharingStoreAutomation> automations, boolean forcedDryRun) {
        int evaluated=0, confirmed=0, declined=0, failed=0; List<String> messages = new ArrayList<>();
        if (automations.isEmpty()) {
            log.info("Foodsharing automation run skipped (dryRun={}, automations=0)", forcedDryRun);
            return new FoodsharingPickupModels.RunResult(0, 0, 0, 0, forcedDryRun, messages);
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
        log.info("Foodsharing automation run started (dryRun={}, automations={}, connections={})", forcedDryRun, automations.size(), connectionSummary);
        Map<String, List<FoodsharingPickupModels.Pickup>> initialPickupsCache = new HashMap<>();
        Map<String, List<FoodsharingPickupModels.Pickup>> livePickupsCache = new HashMap<>();
        boolean effectiveDryRunUsed = forcedDryRun;
        for (FoodsharingStoreAutomation a : automations) {
            boolean effectiveDryRun = forcedDryRun || a.isDryRunEnabled();
            effectiveDryRunUsed = effectiveDryRunUsed || effectiveDryRun;
            String cacheKey = cacheKey(a.getAdminConnection(), a.getStoreId());
            log.info("Foodsharing automation evaluating storeId={} storeName={} admin={} foodsharingUserId={} email={} dryRun={}", a.getStoreId(), a.getStoreName(), a.getAdminConnection().getAdminUser().getId(), a.getAdminConnection().getFoodsharingUserId(), a.getAdminConnection().getFoodsharingEmail(), effectiveDryRun);
            List<FoodsharingPickupModels.Pickup> storePickups = storePickups(a.getAdminConnection(), a.getStoreId()).stream()
                    .sorted(Comparator.comparing(FoodsharingPickupModels.Pickup::date))
                    .toList();
            initialPickupsCache.putIfAbsent(cacheKey, copyPickups(storePickups));
            livePickupsCache.putIfAbsent(cacheKey, copyPickups(storePickups));
            for (var p : storePickups) for (var u : p.users()) if (!u.confirmed()) {
            evaluated++;
            try {
                var decision = evaluate(a, p.date(), u.id(), livePickupsCache, initialPickupsCache); String reason = String.join("\n", decision.reasons());
                String userMessage = decision.userMessage() != null ? decision.userMessage() : (decision.allowed() ? null : reason);
                if (decision.allowed()) {
                    log.info("Foodsharing automation decision=confirm dryRun={} admin={} foodsharingUserId={} email={} storeId={} pickupDate={} userId={} userName={}", effectiveDryRun, a.getAdminConnection().getAdminUser().getId(), a.getAdminConnection().getFoodsharingUserId(), a.getAdminConnection().getFoodsharingEmail(), a.getStoreId(), p.date(), u.id(), u.name());
                    if (!effectiveDryRun) {
                        client.confirm(a.getAdminConnection(), a.getStoreId(), p.date(), u.id());
                        if (userMessage != null && !userMessage.isBlank()) {
                            messageService.send(u.id(), "Fairteilerreinigung", userMessage);
                        }
                    }
                    confirmed++;
                    saveAudit(a, u.id(), u.name(), p.date(), effectiveDryRun, effectiveDryRun ? FoodsharingPickupAutomationDecision.WOULD_CONFIRM : FoodsharingPickupAutomationDecision.CONFIRMED, reason, userMessage, null);
                    updateLiveCache(livePickupsCache, cacheKey, p.date(), u.id(), true);
                }
                else {
                    log.info("Foodsharing automation decision=decline dryRun={} admin={} foodsharingUserId={} email={} storeId={} pickupDate={} userId={} userName={}", effectiveDryRun, a.getAdminConnection().getAdminUser().getId(), a.getAdminConnection().getFoodsharingUserId(), a.getAdminConnection().getFoodsharingEmail(), a.getStoreId(), p.date(), u.id(), u.name());
                    if (!effectiveDryRun) client.decline(a.getAdminConnection(), a.getStoreId(), p.date(), u.id(), reason);
                    declined++;
                    saveAudit(a, u.id(), u.name(), p.date(), effectiveDryRun, effectiveDryRun ? FoodsharingPickupAutomationDecision.WOULD_DECLINE : FoodsharingPickupAutomationDecision.DECLINED, reason, userMessage, null);
                    updateLiveCache(livePickupsCache, cacheKey, p.date(), u.id(), false);
                }
            } catch (RuntimeException ex) { failed++; messages.add(ex.getMessage()); saveAudit(a, u.id(), u.name(), p.date(), effectiveDryRun, FoodsharingPickupAutomationDecision.FAILED, "Automation failed", null, ex.getMessage()); }
            }
        }
        log.info("Foodsharing automation run finished (evaluated={}, confirmed={}, declined={}, failed={}, dryRun={}, connections={})", evaluated, confirmed, declined, failed, effectiveDryRunUsed, connectionSummary);
        return new FoodsharingPickupModels.RunResult(evaluated, confirmed, declined, failed, effectiveDryRunUsed, messages);
    }

    private FoodsharingPickupModels.Decision evaluate(FoodsharingStoreAutomation a, Instant pickupDate, String userId, Map<String, List<FoodsharingPickupModels.Pickup>> livePickupsCache, Map<String, List<FoodsharingPickupModels.Pickup>> initialPickupsCache) {
        List<String> reasons = new ArrayList<>();
        if (pickupDate.isBefore(Instant.now().plus(Duration.ofHours(72)))) {
            reasons.add("Die Abholung ist weniger als 72 Stunden entfernt; die Automationsregeln werden übersprungen.");
            return new FoodsharingPickupModels.Decision(true, reasons, null);
        }
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
        if (a.isCleaningRuleEnabled() && !cleaningRuleExemptionRepository.existsByBezirkAndFoodsharingId(a.getBezirk(), userId)) {
            Long cleaningStoreId = a.getBezirk().getCleaningStoreId();
            if (cleaningStoreId == null) {
                reasons.add("Für den Bezirk " + a.getBezirk().getName() + " ist kein Reinigungs-Fairteiler konfiguriert.");
            } else {
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
                boolean future = cleaningPickups.stream()
                        .filter(p -> p.users().stream().anyMatch(u -> u.id().equals(userId)))
                        .anyMatch(p -> !p.date().isBefore(now));
                if (!past && !future) {
                    long freeCleaningSlots = cleaningPickups.stream()
                            .filter(p -> !p.date().isBefore(now))
                            .filter(p -> p.users().isEmpty())
                            .count();
                    String openCleaningSlots = formatOpenCleaningSlots(cleaningPickups, now);
                    String cleaningOverrideMessage = "Du warst schon länger nicht mehr einen Fairteiler putzen. Wir haben dir den Slot im Betrieb " + a.getStoreName() + " am " + formatSwissDateTime(pickupDate) + " erlaubt, da es aktuell nur " + freeCleaningSlots + " freie Reinigungsslots gibt bei der Fairteilerreinigung. Buch dir doch einen der freien Reinigungsslots: " + openCleaningSlots + ".";
                    if (freeCleaningSlots < appProperties.getFoodsharing().getAutomation().getMinimumFreeCleaningSlots()) {
                        reasons.add(cleaningOverrideMessage);
                    } else {
                        String historyText;
                        if (lastCleaning == null) {
                            historyText = "Du hast bisher noch keinen Fairteiler gereinigt.";
                        } else {
                            long lastCleaningMonths = monthsAgo(lastCleaning, now);
                            historyText = "Deine letzte Reinigung ist " + lastCleaningMonths + monthSuffix(lastCleaningMonths) + " her.";
                        }
                        reasons.add("Du hast in den letzten " + backCheckMonths + monthSuffix(backCheckMonths) + " keinen Fairteiler geputzt. " + historyText + " Zudem hast du keine zukünftige Reinigung geplant. Bitte trage dich für eine Reinigung im Betrieb " + a.getStoreName() + " ein, damit weitere Abholungen geplant werden können. Buch dir doch einen der freien Reinigungsslots: " + openCleaningSlots + ".");
                    }
                }
            }
        }
        if (reasons.isEmpty()) reasons.add("All enabled pickup automation rules passed.");
        String cleaningOverrideMessage = reasons.stream()
                .filter(reason -> reason.startsWith("Du warst schon länger nicht mehr einen Fairteiler putzen."))
                .findFirst()
                .orElse(null);
        boolean onlyAllowedReasons = reasons.stream().allMatch(reason -> reason.startsWith("All enabled") || reason.equals(cleaningOverrideMessage));
        return new FoodsharingPickupModels.Decision(onlyAllowedReasons, reasons, reasons.contains(cleaningOverrideMessage) ? cleaningOverrideMessage : null);
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

    private CleaningRuleExemptionView view(FoodsharingCleaningRuleExemption exemption) { return new CleaningRuleExemptionView(exemption.getId(), exemption.getFoodsharingId(), exemption.getReason()); }
    private AdvertisementAutomationView advertisementView(FoodsharingOpenSlotAdvertisementAutomation a, boolean editable) {
        return new AdvertisementAutomationView(a.getStoreId(), a.getStoreName(), a.getAdvertNumber(), a.isEnabled(), a.getTriggerHoursBefore(), a.isSendToStoreChat(), a.isSendToTelegram(), a.getTelegramChatId(), deserializeMessages(a.getMessagesJson()), editable);
    }

    private String serializeMessages(List<String> messages) {
        try { return objectMapper.writeValueAsString(messages == null ? List.of() : messages.stream().map(this::normalize).filter(v -> !v.isBlank()).toList()); }
        catch (Exception ex) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of("Could not store advertisement messages.")); }
    }

    private List<String> deserializeMessages(String json) {
        try { return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json, new TypeReference<List<String>>() {}); }
        catch (Exception ex) { return List.of(); }
    }

    private String renderAdvertisement(String template, Instant pickupDate, String adminFoodsharingId) {
        var zdt = pickupDate.atZone(SWISS_ZONE);
        String normalizedAdminFoodsharingId = adminFoodsharingId == null ? "" : adminFoodsharingId.trim();
        String adminProfileUrl = normalizedAdminFoodsharingId.isBlank() ? "" : "https://foodsharing.de/user/" + normalizedAdminFoodsharingId + "/profile";
        String time = zdt.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString();
        String dateDe = zdt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN));
        String weekday = zdt.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN));
        return normalize(template).replace("{{date}}", zdt.toLocalDate().toString())
                .replace("{{dateDe}}", dateDe)
                .replace("{{weekday}}", weekday)
                .replace("{{time}}", time)
                .replace("{{datetime}}", zdt.toLocalDate() + " " + time)
                .replace("{{datetimeDe}}", weekday + ", " + dateDe + " um " + time)
                .replace("{{adminFoodsharingId}}", normalizedAdminFoodsharingId)
                .replace("{{adminProfileUrl}}", adminProfileUrl);
    }

    private String formatSwissDateTime(Instant instant) {
        var zdt = instant.atZone(SWISS_ZONE);
        return zdt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN));
    }

    private String formatOpenCleaningSlots(List<FoodsharingPickupModels.Pickup> cleaningPickups, Instant now) {
        List<String> openSlots = cleaningPickups.stream()
                .filter(pickup -> !pickup.date().isBefore(now))
                .filter(pickup -> pickup.users().isEmpty())
                .sorted(Comparator.comparing(FoodsharingPickupModels.Pickup::date))
                .limit(3)
                .map(pickup -> formatSwissDateTime(pickup.date()))
                .toList();
        return openSlots.isEmpty() ? "keine freien Reinigungsslots" : String.join(", ", openSlots);
    }

    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private String blankToNull(String value) { String normalized = normalize(value); return normalized.isBlank() ? null : normalized; }

    private String normalizeRequired(String value) { if (value == null || value.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED); return value.trim(); }
    private void saveAudit(FoodsharingStoreAutomation a, String userId, String userName, Instant pickupDate, boolean dryRun, FoodsharingPickupAutomationDecision decision, String reasons, String userMessage, String error) { var audit = new FoodsharingPickupAutomationAudit(); audit.setBezirk(a.getBezirk()); audit.setAdminConnection(a.getAdminConnection()); audit.setStoreId(a.getStoreId()); audit.setFoodsharingUserId(userId); audit.setFoodsharingUserName(userName == null || userName.isBlank() ? null : userName); audit.setPickupDate(pickupDate); audit.setDryRun(dryRun); audit.setDecision(decision); audit.setReasons(reasons); audit.setUserMessage(userMessage); audit.setFoodsharingError(error); auditRepository.save(audit); }
    private FoodsharingAdminConnection requireConnection() { return connectionRepository.findByAdminUser(currentActorService.requireAutomationUser()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED)); }
    private Bezirk requireBezirk(User user) { if (user.getBezirk() == null) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("User has no Bezirk.")); return user.getBezirk(); }
    private Bezirk requireBezirk(String bezirkSlug, User user) {
        Bezirk requested = bezirkService.requireActive(bezirkSlug);
        if (user.isCanManageUsers()) {
            return requested;
        }
        Bezirk assigned = requireBezirk(user);
        if (!assigned.getId().equals(requested.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BEZIRK_NOT_FOUND);
        }
        return requested;
    }
    private StoreAutomationView view(long storeId, String storeName, FoodsharingStoreAutomation automation, FoodsharingAdminConnection currentConnection) {
        if (automation == null) {
            return new StoreAutomationView(storeId, storeName, false, true, false, 0, false, false, true, null, null);
        }
        User owner = automation.getAdminConnection().getAdminUser();
        return new StoreAutomationView(
                storeId,
                storeName,
                automation.isEnabled(),
                automation.isDryRunEnabled(),
                automation.isGapRuleEnabled(),
                automation.getMinimumGapDays(),
                automation.isCleaningRuleEnabled(),
                automation.isExperienceRuleEnabled(),
                automation.getAdminConnection().getId().equals(currentConnection.getId()),
                owner == null ? null : owner.getName(),
                owner == null ? null : owner.getEmail());
    }
    private AuditView view(FoodsharingPickupAutomationAudit a, Map<Long, String> storeNames) { return new AuditView(a.getStoreId(), storeNames.getOrDefault(a.getStoreId(), String.valueOf(a.getStoreId())), a.getFoodsharingUserId(), a.getFoodsharingUserName(), a.getPickupDate(), a.isDryRun(), a.getDecision().name(), a.getReasons(), a.getUserMessage(), a.getFoodsharingError(), a.getCreatedAt()); }
    private long monthsAgo(Instant value, Instant now) { return value == null ? 0 : ChronoUnit.MONTHS.between(value.atZone(SWISS_ZONE).toLocalDate(), now.atZone(SWISS_ZONE).toLocalDate()); }
    private String monthSuffix(long months) { return months == 1 ? " Monat" : " Monaten"; }
    private String daySuffix(long days) { return days == 1 ? " Tag" : " Tage"; }

    public record CleaningRuleExemptionRequest(String foodsharingId, String reason) {}
    public record CleaningRuleExemptionView(UUID id, String foodsharingId, String reason) {}
    public record ConnectionStatus(boolean connected, String email, String foodsharingUserId, Instant authenticatedAt, boolean telegramBotTokenConfigured, boolean automationEnabled, boolean automationDryRun) {}
    public record ManagedStoreView(long storeId, String storeName) {}
    public record StoreOverviewView(List<StoreAutomationView> automations, List<ManagedStoreView> availableStores) {}
    public record StoreAutomationRequest(String storeName, boolean enabled, boolean dryRunEnabled, boolean gapRuleEnabled, int minimumGapDays, boolean cleaningRuleEnabled, boolean experienceRuleEnabled) {}
    public record StoreAutomationView(long storeId, String storeName, boolean enabled, boolean dryRunEnabled, boolean gapRuleEnabled, int minimumGapDays, boolean cleaningRuleEnabled, boolean experienceRuleEnabled, boolean editable, String ownerName, String ownerEmail) {}
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

    @Transactional(readOnly = true)
    public List<ExtraAutomationAuditView> extraAutomationAudit() {
        List<ExtraAutomationAuditView> result = new ArrayList<>();
        result.addAll(requestAutomationAudit());
        result.addAll(openSlotAdvertisementAudit());
        return result.stream().sorted(Comparator.comparing(ExtraAutomationAuditView::createdAt).reversed()).limit(100).toList();
    }

    @Transactional(readOnly = true)
    public List<ExtraAutomationAuditView> requestAutomationAudit() {
        return requestAutomationAuditRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(a -> new ExtraAutomationAuditView("REQUEST", a.getStoreId(), a.getStoreName(), null, a.getFoodsharingUserId(), a.getFoodsharingUserName(), a.isDryRun(), a.getStatus(), a.getReason(), null, a.getError(), a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExtraAutomationAuditView> openSlotAdvertisementAudit() {
        return advertisementAuditRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(a -> new ExtraAutomationAuditView("ADVERTISEMENT", a.getStoreId(), a.getStoreName(), a.getPickupDate(), null, null, a.isDryRun(), a.getStatus(), a.getReason(), a.getMessage(), a.getError(), a.getCreatedAt()))
                .toList();
    }

    public record RequestAutomationRequest(String storeName, boolean enabled, boolean dryRunEnabled, boolean distanceRuleEnabled, Double maximumDistanceKm) {}
    public record RequestAutomationView(long storeId, String storeName, boolean enabled, boolean dryRunEnabled, boolean distanceRuleEnabled, double maximumDistanceKm, boolean editable) {}
    public record AdvertisementAutomationRequest(String storeName, boolean enabled, int triggerHoursBefore, boolean sendToStoreChat, boolean sendToTelegram, String telegramChatId, List<String> messages) {}
    public record AdvertisementAutomationView(long storeId, String storeName, int advertNumber, boolean enabled, int triggerHoursBefore, boolean sendToStoreChat, boolean sendToTelegram, String telegramChatId, List<String> messages, boolean editable) {}
    public record TelegramChatView(String id, String title, String type) {}
    public record AutomationRunSummary(int evaluated, int acted, int skipped, boolean dryRun, List<String> messages) {}
    public record ExtraAutomationOverviewView(List<RequestAutomationView> requestAutomations, List<AdvertisementAutomationView> advertisementAutomations) {}
    public record ExtraAutomationAuditView(String automationType, long storeId, String storeName, Instant pickupDate, String foodsharingUserId, String foodsharingUserName, boolean dryRun, String status, String reason, String message, String error, Instant createdAt) {}
}
