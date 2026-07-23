package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.AdminApi;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminBezirkResponse;
import ch.it4user.foodsharing.openapi.model.AutomationRunSummary;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.FoodsharingAutomationAudit;
import ch.it4user.foodsharing.openapi.model.FoodsharingCleaningRuleExemption;
import ch.it4user.foodsharing.openapi.model.FoodsharingCleaningRuleExemptionRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingConnectRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingConnectionStatus;
import ch.it4user.foodsharing.openapi.model.FoodsharingExtraAutomationAudit;
import ch.it4user.foodsharing.openapi.model.FoodsharingExtraAutomationOverview;
import ch.it4user.foodsharing.openapi.model.FoodsharingFuturePickup;
import ch.it4user.foodsharing.openapi.model.FoodsharingFuturePickupUser;
import ch.it4user.foodsharing.openapi.model.FoodsharingOpenSlotAdvertisementAutomation;
import ch.it4user.foodsharing.openapi.model.FoodsharingOpenSlotAdvertisementAutomationRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingOpenSlotAdvertisementOverview;
import ch.it4user.foodsharing.openapi.model.FoodsharingRequestAutomation;
import ch.it4user.foodsharing.openapi.model.FoodsharingRequestAutomationRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingRequestAutomationOverview;
import ch.it4user.foodsharing.openapi.model.FoodsharingRunRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingRunResult;
import ch.it4user.foodsharing.openapi.model.FoodsharingManagedStore;
import ch.it4user.foodsharing.openapi.model.FoodsharingTelegramBotTokenRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomationOverview;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomationRequest;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.openapi.model.TelegramChat;
import ch.it4user.foodsharing.openapi.model.TelegramTestMessageRequest;
import ch.it4user.foodsharing.openapi.model.UserPermissionsRequest;
import ch.it4user.foodsharing.openapi.model.UpdateBezirkRequest;
import ch.it4user.foodsharing.openapi.model.UpdateUserBezirkRequest;
import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.service.AdminService;
import ch.it4user.foodsharing.service.ApiErrorCode;
import ch.it4user.foodsharing.service.ApiException;
import ch.it4user.foodsharing.service.BezirkService;
import ch.it4user.foodsharing.service.CurrentActorService;
import ch.it4user.foodsharing.service.FoodsharingPickupAutomationService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController implements AdminApi {

    private final AdminService adminService;
    private final ApiModelMapper mapper;
    private final FoodsharingPickupAutomationService foodsharingPickupAutomationService;
    private final CurrentActorService currentActorService;
    private final BezirkService bezirkService;

    public AdminController(AdminService adminService,
                           ApiModelMapper mapper,
                           FoodsharingPickupAutomationService foodsharingPickupAutomationService,
                           CurrentActorService currentActorService,
                           BezirkService bezirkService) {
        this.adminService = adminService;
        this.mapper = mapper;
        this.foodsharingPickupAutomationService = foodsharingPickupAutomationService;
        this.currentActorService = currentActorService;
        this.bezirkService = bezirkService;
    }

    @Override
    public ResponseEntity<TeacherListResponse> getAdminTeachers(String bezirkSlug, Integer page, Integer size) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherListResponse(
                adminService.getTeachers(bezirkSlug, page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<TeacherListResponse> getAdminAdmins(Integer page, Integer size) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherListResponse(adminService.getAdmins(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<TeacherResponse> enableAdminTeacher(UUID teacherId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherActive(teacherId, true)));
    }

    @Override
    public ResponseEntity<TeacherResponse> disableAdminTeacher(UUID teacherId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherActive(teacherId, false)));
    }

    @Override
    public ResponseEntity<TeacherResponse> grantAdminTeacher(UUID teacherId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherAdmin(teacherId, true)));
    }

    @Override
    public ResponseEntity<TeacherResponse> revokeAdminTeacher(UUID teacherId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherAdmin(teacherId, false)));
    }

    @Override
    public ResponseEntity<AdminBookingUserPageResponse> getAdminUsers(
            String bezirkSlug,
            Integer page,
            Integer size,
            Boolean threePickupsOnly,
            Boolean activeOnly,
            Boolean unassigned,
            Boolean allBezirke) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 50 : size;
        boolean resolvedThreePickupsOnly = threePickupsOnly != null && threePickupsOnly;
        boolean resolvedActiveOnly = activeOnly == null || activeOnly;
        return ResponseEntity.ok(mapper.toAdminBookingUserPageResponse(
                adminService.getUsers(
                        bezirkSlug,
                        Boolean.TRUE.equals(unassigned),
                        Boolean.TRUE.equals(allBezirke),
                        resolvedPage,
                        resolvedSize,
                        resolvedThreePickupsOnly,
                        resolvedActiveOnly)));
    }

    @Override
    public ResponseEntity<AdminBezirkResponse> getAdminBezirk(String bezirkSlug) {
        User automationUser = null;
        if (!currentActorService.hasPermission(UserPermission.CAN_MANAGE_USERS)) {
            automationUser = currentActorService.requireAutomationUser();
        }
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        if (automationUser != null && (automationUser.getBezirk() == null
                || !automationUser.getBezirk().getId().equals(bezirk.getId()))) {
            throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BEZIRK_NOT_FOUND);
        }
        return ResponseEntity.ok(mapper.toAdminBezirkResponse(bezirk));
    }

    @Override
    public ResponseEntity<AdminBezirkResponse> updateAdminBezirk(
            String bezirkSlug,
            UpdateBezirkRequest updateBezirkRequest) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toAdminBezirkResponse(
                bezirkService.updateCleaningStoreId(bezirkSlug, updateBezirkRequest.getCleaningStoreId())));
    }

    @Override
    public ResponseEntity<BookingUserResponse> enableAdminBookingUser(UUID bookingUserId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toBookingUserResponse(adminService.enableBookingUser(bookingUserId)));
    }

    @Override
    public ResponseEntity<BookingUserResponse> disableAdminBookingUser(UUID bookingUserId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toBookingUserResponse(adminService.disableBookingUser(bookingUserId)));
    }

    @Override
    public ResponseEntity<BookingUserResponse> grantAdminBookingUser(UUID bookingUserId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toBookingUserResponse(adminService.grantBookingUserAdmin(bookingUserId)));
    }

    @Override
    public ResponseEntity<TeacherResponse> enableAdminUser(UUID adminUserId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setAdminActive(adminUserId, true)));
    }

    @Override
    public ResponseEntity<TeacherResponse> disableAdminUser(UUID adminUserId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setAdminActive(adminUserId, false)));
    }

    @Override
    public ResponseEntity<TeacherResponse> revokeAdminUser(UUID adminUserId) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setAdminAdmin(adminUserId, false)));
    }


    @Override
    public ResponseEntity<TeacherResponse> setAdminUserPermissions(UUID userId, UserPermissionsRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        AdminService.UserPermissions permissions = new AdminService.UserPermissions(
                Boolean.TRUE.equals(request.getCanGiveEinAbs()),
                Boolean.TRUE.equals(request.getCanManageUsers()),
                Boolean.TRUE.equals(request.getCanUseAutomations()),
                Boolean.TRUE.equals(request.getCanSeeUserPickupCountGrouping()),
                Boolean.TRUE.equals(request.getCanUseAutomationSlotApproval()),
                Boolean.TRUE.equals(request.getCanUseAutomationRequestApproval()),
                Boolean.TRUE.equals(request.getCanUseAutomationOpenSlotAdvertising()),
                Boolean.TRUE.equals(request.getCanSeeAllAutomationDecisions()));
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setUserPermissions(userId, permissions)));
    }

    @Override
    public ResponseEntity<BookingUserResponse> updateAdminUserBezirk(UUID userId, UpdateUserBezirkRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toBookingUserResponse(
                adminService.setUserBezirk(userId, request == null ? null : request.getBezirkSlug())));
    }

    @Override
    public ResponseEntity<FoodsharingConnectionStatus> connectFoodsharing(FoodsharingConnectRequest request) {
        currentActorService.requireAutomationUser();
        return ResponseEntity.ok(toFoodsharingConnectionStatus(
                foodsharingPickupAutomationService.connect(request.getEmail(), request.getPassword(), request.getTelegramBotToken())));
    }

    @Override
    public ResponseEntity<Void> disconnectFoodsharing() {
        currentActorService.requireAutomationUser();
        foodsharingPickupAutomationService.disconnect();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> saveFoodsharingTelegramBotToken(FoodsharingTelegramBotTokenRequest request) {
        currentActorService.requireAutomationUser();
        foodsharingPickupAutomationService.saveTelegramBotToken(request.getTelegramBotToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FoodsharingConnectionStatus> getFoodsharingStatus() {
        currentActorService.requireAutomationUser();
        return ResponseEntity.ok(toFoodsharingConnectionStatus(foodsharingPickupAutomationService.status()));
    }

    @Override
    public ResponseEntity<FoodsharingStoreAutomationOverview> getFoodsharingStores(String bezirkSlug) {
        currentActorService.requireAutomationUser();
        FoodsharingPickupAutomationService.StoreOverviewView overview = foodsharingPickupAutomationService.stores(bezirkSlug);
        FoodsharingStoreAutomationOverview response = new FoodsharingStoreAutomationOverview();
        response.setAutomations(overview.automations().stream()
                .map(this::toFoodsharingStoreAutomation)
                .toList());
        response.setAvailableStores(overview.availableStores().stream()
                .map(this::toFoodsharingManagedStore)
                .toList());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FoodsharingStoreAutomation> saveFoodsharingStoreAutomation(
            String bezirkSlug,
            Long storeId,
            FoodsharingStoreAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        FoodsharingPickupAutomationService.StoreAutomationRequest serviceRequest =
                new FoodsharingPickupAutomationService.StoreAutomationRequest(
                        request.getStoreName(),
                        Boolean.TRUE.equals(request.getEnabled()),
                        request.getDryRunEnabled() == null || Boolean.TRUE.equals(request.getDryRunEnabled()),
                        Boolean.TRUE.equals(request.getGapRuleEnabled()),
                        request.getMinimumGapDays() == null ? 0 : request.getMinimumGapDays(),
                        Boolean.TRUE.equals(request.getCleaningRuleEnabled()),
                        Boolean.TRUE.equals(request.getExperienceRuleEnabled()));
        return ResponseEntity.ok(toFoodsharingStoreAutomation(
                foodsharingPickupAutomationService.save(bezirkSlug, storeId, serviceRequest)));
    }

    @Override
    public ResponseEntity<Void> deleteFoodsharingStoreAutomation(String bezirkSlug, Long storeId) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        foodsharingPickupAutomationService.delete(bezirkSlug, storeId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FoodsharingRunResult> runFoodsharingAutomation(String bezirkSlug, FoodsharingRunRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return ResponseEntity.ok(toFoodsharingRunResult(
                foodsharingPickupAutomationService.run(bezirkSlug, Boolean.TRUE.equals(request.getDryRun()))));
    }

    @Override
    public ResponseEntity<FoodsharingRequestAutomationOverview> getFoodsharingRequestAutomationOverview() {
        currentActorService.requireAutomationUser();
        return ResponseEntity.ok(toFoodsharingRequestAutomationOverview(foodsharingPickupAutomationService.requestAutomationOverview()));
    }

    @Override
    public ResponseEntity<FoodsharingOpenSlotAdvertisementOverview> getFoodsharingOpenSlotAdvertisementOverview() {
        currentActorService.requireAutomationUser();
        return ResponseEntity.ok(toFoodsharingOpenSlotAdvertisementOverview(foodsharingPickupAutomationService.openSlotAdvertisementOverview()));
    }

    @Override
    public ResponseEntity<FoodsharingExtraAutomationOverview> getFoodsharingExtraAutomationOverview() {
        currentActorService.requireAutomationUser();
        return ResponseEntity.ok(toFoodsharingExtraAutomationOverview(foodsharingPickupAutomationService.extraAutomationOverview()));
    }

    @Override
    public ResponseEntity<List<FoodsharingExtraAutomationAudit>> getFoodsharingRequestAutomationAudit(Boolean onlyMine) {
        requireAuditAccess(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        return ResponseEntity.ok(foodsharingPickupAutomationService.requestAutomationAudit(Boolean.TRUE.equals(onlyMine)).stream()
                .map(this::toFoodsharingExtraAutomationAudit)
                .toList());
    }

    @Override
    public ResponseEntity<List<FoodsharingExtraAutomationAudit>> getFoodsharingOpenSlotAdvertisementAudit(Boolean onlyMine) {
        requireAuditAccess(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        return ResponseEntity.ok(foodsharingPickupAutomationService.openSlotAdvertisementAudit(Boolean.TRUE.equals(onlyMine)).stream()
                .map(this::toFoodsharingExtraAutomationAudit)
                .toList());
    }

    @Override
    public ResponseEntity<List<FoodsharingExtraAutomationAudit>> getFoodsharingExtraAutomationAudit(Boolean onlyMine) {
        currentActorService.requireAutomationUser();
        return ResponseEntity.ok(foodsharingPickupAutomationService.extraAutomationAudit(Boolean.TRUE.equals(onlyMine)).stream()
                .map(this::toFoodsharingExtraAutomationAudit)
                .toList());
    }

    @Override
    public ResponseEntity<List<TelegramChat>> getFoodsharingTelegramChats() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        return ResponseEntity.ok(foodsharingPickupAutomationService.telegramChats().stream()
                .map(this::toTelegramChat)
                .toList());
    }

    @Override
    public ResponseEntity<AutomationRunSummary> runFoodsharingRequestAutomationDryRun() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        return ResponseEntity.ok(toAutomationRunSummary(foodsharingPickupAutomationService.runRequestAutomations(true)));
    }

    @Override
    public ResponseEntity<AutomationRunSummary> runFoodsharingOpenSlotAdvertisementDryRun() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        return ResponseEntity.ok(toAutomationRunSummary(foodsharingPickupAutomationService.runAdvertisementAutomations(true)));
    }

    @Override
    public ResponseEntity<Void> sendFoodsharingTelegramTestMessage(TelegramTestMessageRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        foodsharingPickupAutomationService.sendTelegramTestMessage(request.getChatId(), request.getMessage());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FoodsharingRequestAutomation> saveFoodsharingRequestAutomation(Long storeId, FoodsharingRequestAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        FoodsharingPickupAutomationService.RequestAutomationRequest serviceRequest =
                new FoodsharingPickupAutomationService.RequestAutomationRequest(
                        request.getStoreName(),
                        Boolean.TRUE.equals(request.getEnabled()),
                        Boolean.TRUE.equals(request.getDryRunEnabled()),
                        Boolean.TRUE.equals(request.getDistanceRuleEnabled()),
                        request.getMaximumDistanceKm());
        return ResponseEntity.ok(toFoodsharingRequestAutomation(
                foodsharingPickupAutomationService.saveRequestAutomation(storeId, serviceRequest)));
    }

    @Override
    public ResponseEntity<Void> deleteFoodsharingRequestAutomation(Long storeId) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        foodsharingPickupAutomationService.deleteRequestAutomation(storeId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FoodsharingOpenSlotAdvertisementAutomation> saveFoodsharingOpenSlotAdvertisementAutomation(
            Long storeId,
            Integer advertNumber,
            FoodsharingOpenSlotAdvertisementAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        FoodsharingPickupAutomationService.AdvertisementAutomationRequest serviceRequest =
                new FoodsharingPickupAutomationService.AdvertisementAutomationRequest(
                        request.getStoreName(),
                        Boolean.TRUE.equals(request.getEnabled()),
                        request.getTriggerHoursBefore() == null ? 0 : request.getTriggerHoursBefore(),
                        Boolean.TRUE.equals(request.getSendToStoreChat()),
                        Boolean.TRUE.equals(request.getSendToTelegram()),
                        request.getTelegramChatId(),
                        request.getStoreMessages() == null ? List.of() : request.getStoreMessages(),
                        request.getTelegramMessages() == null ? List.of() : request.getTelegramMessages());
        return ResponseEntity.ok(toFoodsharingOpenSlotAdvertisementAutomation(
                foodsharingPickupAutomationService.saveAdvertisementAutomation(storeId, advertNumber, serviceRequest)));
    }

    @Override
    public ResponseEntity<Void> deleteFoodsharingOpenSlotAdvertisementAutomation(Long storeId, Integer advertNumber) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        foodsharingPickupAutomationService.deleteAdvertisementAutomation(storeId, advertNumber);
        return ResponseEntity.noContent().build();
    }


    @Override
    public ResponseEntity<List<FoodsharingCleaningRuleExemption>> getFoodsharingCleaningRuleExemptions(String bezirkSlug) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return ResponseEntity.ok(foodsharingPickupAutomationService.cleaningRuleExemptions(bezirkSlug).stream()
                .map(this::toFoodsharingCleaningRuleExemption)
                .toList());
    }

    @Override
    public ResponseEntity<FoodsharingCleaningRuleExemption> saveFoodsharingCleaningRuleExemption(
            String bezirkSlug,
            FoodsharingCleaningRuleExemptionRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        FoodsharingPickupAutomationService.CleaningRuleExemptionRequest serviceRequest =
                new FoodsharingPickupAutomationService.CleaningRuleExemptionRequest(request.getFoodsharingId(), request.getReason());
        return ResponseEntity.ok(toFoodsharingCleaningRuleExemption(
                foodsharingPickupAutomationService.saveCleaningRuleExemption(bezirkSlug, serviceRequest)));
    }

    @Override
    public ResponseEntity<Void> deleteFoodsharingCleaningRuleExemption(String bezirkSlug, UUID exemptionId) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        foodsharingPickupAutomationService.deleteCleaningRuleExemption(bezirkSlug, exemptionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<FoodsharingAutomationAudit>> getFoodsharingAutomationAudit(String bezirkSlug, Boolean onlyMine) {
        requireAuditAccess(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return ResponseEntity.ok(foodsharingPickupAutomationService.audit(bezirkSlug, Boolean.TRUE.equals(onlyMine)).stream()
                .map(this::toFoodsharingAutomationAudit)
                .toList());
    }

    @Override
    public ResponseEntity<List<FoodsharingFuturePickupUser>> getFoodsharingFuturePickupUsers(String bezirkSlug) {
        currentActorService.requirePermission(UserPermission.CAN_SEE_USER_PICKUP_COUNT_GROUPING);
        return ResponseEntity.ok(foodsharingPickupAutomationService.futurePickupUsers(bezirkSlug).stream()
                .map(this::toFoodsharingFuturePickupUser)
                .toList());
    }

    private void requireAuditAccess(UserPermission automationPermission) {
        if (!currentActorService.hasPermission(UserPermission.CAN_SEE_ALL_AUTOMATION_DECISIONS)) {
            currentActorService.requirePermission(automationPermission);
        }
    }

    private FoodsharingConnectionStatus toFoodsharingConnectionStatus(FoodsharingPickupAutomationService.ConnectionStatus status) {
        FoodsharingConnectionStatus response = new FoodsharingConnectionStatus();
        response.setConnected(status.connected());
        response.setEmail(status.email());
        response.setFoodsharingUserId(status.foodsharingUserId());
        response.setAuthenticatedAt(toOffsetDateTime(status.authenticatedAt()));
        response.setTelegramBotTokenConfigured(status.telegramBotTokenConfigured());
        response.setAutomationEnabled(status.automationEnabled());
        response.setAutomationDryRun(status.automationDryRun());
        return response;
    }

    private FoodsharingStoreAutomation toFoodsharingStoreAutomation(FoodsharingPickupAutomationService.StoreAutomationView store) {
        FoodsharingStoreAutomation response = new FoodsharingStoreAutomation();
        response.setStoreId(store.storeId());
        response.setStoreName(store.storeName());
        response.setEnabled(store.enabled());
        response.setDryRunEnabled(store.dryRunEnabled());
        response.setGapRuleEnabled(store.gapRuleEnabled());
        response.setMinimumGapDays(store.minimumGapDays());
        response.setCleaningRuleEnabled(store.cleaningRuleEnabled());
        response.setExperienceRuleEnabled(store.experienceRuleEnabled());
        response.setEditable(store.editable());
        response.setOwnerName(store.ownerName());
        response.setOwnerEmail(store.ownerEmail());
        return response;
    }

    private FoodsharingManagedStore toFoodsharingManagedStore(FoodsharingPickupAutomationService.ManagedStoreView store) {
        FoodsharingManagedStore response = new FoodsharingManagedStore();
        response.setStoreId(store.storeId());
        response.setStoreName(store.storeName());
        return response;
    }

    private FoodsharingRunResult toFoodsharingRunResult(ch.it4user.foodsharing.service.FoodsharingPickupModels.RunResult result) {
        FoodsharingRunResult response = new FoodsharingRunResult();
        response.setEvaluated(result.evaluated());
        response.setConfirmed(result.confirmed());
        response.setDeclined(result.declined());
        response.setFailed(result.failed());
        response.setDryRun(result.dryRun());
        response.setMessages(result.messages());
        return response;
    }

    private FoodsharingRequestAutomationOverview toFoodsharingRequestAutomationOverview(List<FoodsharingPickupAutomationService.RequestAutomationView> requests) {
        FoodsharingRequestAutomationOverview response = new FoodsharingRequestAutomationOverview();
        response.setRequestAutomations(requests.stream()
                .map(this::toFoodsharingRequestAutomation)
                .toList());
        return response;
    }

    private FoodsharingOpenSlotAdvertisementOverview toFoodsharingOpenSlotAdvertisementOverview(List<FoodsharingPickupAutomationService.AdvertisementAutomationView> advertisements) {
        FoodsharingOpenSlotAdvertisementOverview response = new FoodsharingOpenSlotAdvertisementOverview();
        response.setAdvertisementAutomations(advertisements.stream()
                .map(this::toFoodsharingOpenSlotAdvertisementAutomation)
                .toList());
        return response;
    }

    private FoodsharingExtraAutomationOverview toFoodsharingExtraAutomationOverview(FoodsharingPickupAutomationService.ExtraAutomationOverviewView overview) {
        FoodsharingExtraAutomationOverview response = new FoodsharingExtraAutomationOverview();
        response.setRequestAutomations(overview.requestAutomations().stream()
                .map(this::toFoodsharingRequestAutomation)
                .toList());
        response.setAdvertisementAutomations(overview.advertisementAutomations().stream()
                .map(this::toFoodsharingOpenSlotAdvertisementAutomation)
                .toList());
        return response;
    }

    private FoodsharingRequestAutomation toFoodsharingRequestAutomation(FoodsharingPickupAutomationService.RequestAutomationView requestAutomation) {
        FoodsharingRequestAutomation response = new FoodsharingRequestAutomation();
        response.setStoreId(requestAutomation.storeId());
        response.setStoreName(requestAutomation.storeName());
        response.setEnabled(requestAutomation.enabled());
        response.setDryRunEnabled(requestAutomation.dryRunEnabled());
        response.setDistanceRuleEnabled(requestAutomation.distanceRuleEnabled());
        response.setMaximumDistanceKm(requestAutomation.maximumDistanceKm());
        response.setEditable(requestAutomation.editable());
        return response;
    }

    private FoodsharingOpenSlotAdvertisementAutomation toFoodsharingOpenSlotAdvertisementAutomation(FoodsharingPickupAutomationService.AdvertisementAutomationView advertisement) {
        FoodsharingOpenSlotAdvertisementAutomation response = new FoodsharingOpenSlotAdvertisementAutomation();
        response.setStoreId(advertisement.storeId());
        response.setStoreName(advertisement.storeName());
        response.setAdvertNumber(advertisement.advertNumber());
        response.setEnabled(advertisement.enabled());
        response.setTriggerHoursBefore(advertisement.triggerHoursBefore());
        response.setSendToStoreChat(advertisement.sendToStoreChat());
        response.setSendToTelegram(advertisement.sendToTelegram());
        response.setTelegramChatId(advertisement.telegramChatId());
        response.setStoreMessages(advertisement.storeMessages());
        response.setTelegramMessages(advertisement.telegramMessages());
        response.setEditable(advertisement.editable());
        return response;
    }

    private FoodsharingExtraAutomationAudit toFoodsharingExtraAutomationAudit(FoodsharingPickupAutomationService.ExtraAutomationAuditView audit) {
        FoodsharingExtraAutomationAudit response = new FoodsharingExtraAutomationAudit();
        response.setAutomationType(audit.automationType());
        response.setStoreId(audit.storeId());
        response.setStoreName(audit.storeName());
        response.setPickupDate(toOffsetDateTime(audit.pickupDate()));
        response.setNotificationDate(toOffsetDateTime(audit.notificationDate()));
        response.setFoodsharingUserId(audit.foodsharingUserId());
        response.setFoodsharingUserName(audit.foodsharingUserName());
        response.setDryRun(audit.dryRun());
        response.setStatus(audit.status());
        response.setReason(audit.reason());
        response.setMessage(audit.message());
        response.setError(audit.error());
        response.setCreatedAt(toOffsetDateTime(audit.createdAt()));
        return response;
    }

    private TelegramChat toTelegramChat(FoodsharingPickupAutomationService.TelegramChatView chat) {
        TelegramChat response = new TelegramChat();
        response.setId(chat.id());
        response.setTitle(chat.title());
        response.setType(chat.type());
        return response;
    }

    private AutomationRunSummary toAutomationRunSummary(FoodsharingPickupAutomationService.AutomationRunSummary summary) {
        AutomationRunSummary response = new AutomationRunSummary();
        response.setEvaluated(summary.evaluated());
        response.setActed(summary.acted());
        response.setSkipped(summary.skipped());
        response.setDryRun(summary.dryRun());
        response.setMessages(summary.messages());
        return response;
    }


    private FoodsharingCleaningRuleExemption toFoodsharingCleaningRuleExemption(FoodsharingPickupAutomationService.CleaningRuleExemptionView exemption) {
        FoodsharingCleaningRuleExemption response = new FoodsharingCleaningRuleExemption();
        response.setId(exemption.id());
        response.setFoodsharingId(exemption.foodsharingId());
        response.setReason(exemption.reason());
        return response;
    }

    private FoodsharingAutomationAudit toFoodsharingAutomationAudit(FoodsharingPickupAutomationService.AuditView audit) {
        FoodsharingAutomationAudit response = new FoodsharingAutomationAudit();
        response.setStoreId(audit.storeId());
        response.setStoreName(audit.storeName());
        response.setFoodsharingUserId(audit.foodsharingUserId());
        response.setFoodsharingUserName(audit.foodsharingUserName());
        response.setPickupDate(toOffsetDateTime(audit.pickupDate()));
        response.setDryRun(audit.dryRun());
        response.setDecision(audit.decision());
        response.setReasons(audit.reasons());
        response.setUserMessage(audit.userMessage());
        response.setError(audit.error());
        response.setCreatedAt(toOffsetDateTime(audit.createdAt()));
        return response;
    }

    private FoodsharingFuturePickupUser toFoodsharingFuturePickupUser(FoodsharingPickupAutomationService.StorePickupUserView user) {
        FoodsharingFuturePickupUser response = new FoodsharingFuturePickupUser();
        response.setFoodsharingUserId(user.foodsharingUserId());
        response.setName(user.name());
        response.setFuturePickupCount(user.futurePickupCount());
        response.setFuturePickups(user.futurePickups().stream()
                .map(this::toFoodsharingFuturePickup)
                .toList());
        return response;
    }

    private FoodsharingFuturePickup toFoodsharingFuturePickup(FoodsharingPickupAutomationService.StorePickupView pickup) {
        FoodsharingFuturePickup response = new FoodsharingFuturePickup();
        response.setStoreId(pickup.storeId());
        response.setStoreName(pickup.storeName());
        response.setPickupDate(toOffsetDateTime(pickup.pickupDate()));
        response.setConfirmed(pickup.confirmed());
        return response;
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }

}
