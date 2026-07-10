package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.AdminApi;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.FoodsharingAutomationAudit;
import ch.it4user.foodsharing.openapi.model.FoodsharingCleaningRuleExemption;
import ch.it4user.foodsharing.openapi.model.FoodsharingCleaningRuleExemptionRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingConnectRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingConnectionStatus;
import ch.it4user.foodsharing.openapi.model.FoodsharingFuturePickup;
import ch.it4user.foodsharing.openapi.model.FoodsharingFuturePickupUser;
import ch.it4user.foodsharing.openapi.model.FoodsharingRunRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingRunResult;
import ch.it4user.foodsharing.openapi.model.FoodsharingManagedStore;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomationOverview;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomationRequest;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.openapi.model.UserPermissionsRequest;
import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.service.AdminService;
import ch.it4user.foodsharing.service.CurrentActorService;
import ch.it4user.foodsharing.service.FoodsharingPickupAutomationService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController implements AdminApi {

    private final AdminService adminService;
    private final ApiModelMapper mapper;
    private final FoodsharingPickupAutomationService foodsharingPickupAutomationService;
    private final CurrentActorService currentActorService;

    public AdminController(AdminService adminService, ApiModelMapper mapper, FoodsharingPickupAutomationService foodsharingPickupAutomationService, CurrentActorService currentActorService) {
        this.adminService = adminService;
        this.mapper = mapper;
        this.foodsharingPickupAutomationService = foodsharingPickupAutomationService;
        this.currentActorService = currentActorService;
    }

    @Override
    public ResponseEntity<TeacherListResponse> getAdminTeachers(Integer page, Integer size) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        return ResponseEntity.ok(mapper.toTeacherListResponse(adminService.getTeachers(page == null ? 0 : page, size == null ? 20 : size)));
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
    public ResponseEntity<AdminEinAbListResponse> getAdminEinAbs(Integer page, Integer size) {
        currentActorService.requirePermission(UserPermission.CAN_GIVE_EIN_ABS);
        return ResponseEntity.ok(mapper.toAdminEinAbListResponse(adminService.getEinAbs(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<BookingListResponse> getAdminBookings(Integer page, Integer size) {
        currentActorService.requirePermission(UserPermission.CAN_GIVE_EIN_ABS);
        return ResponseEntity.ok(mapper.toBookingListResponse(adminService.getBookings(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<AdminBookingUserPageResponse> getAdminUsers(Integer page, Integer size, Boolean threePickupsOnly, Boolean activeOnly) {
        currentActorService.requirePermission(UserPermission.CAN_MANAGE_USERS);
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 50 : size;
        boolean resolvedThreePickupsOnly = threePickupsOnly != null && threePickupsOnly;
        boolean resolvedActiveOnly = activeOnly == null || activeOnly;
        return ResponseEntity.ok(mapper.toAdminBookingUserPageResponse(
                adminService.getUsers(resolvedPage, resolvedSize, resolvedThreePickupsOnly, resolvedActiveOnly)));
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
                Boolean.TRUE.equals(request.getCanSeeAllAutomationDecisions()));
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setUserPermissions(userId, permissions)));
    }

    @Override
    public ResponseEntity<FoodsharingConnectionStatus> connectFoodsharing(FoodsharingConnectRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATIONS);
        return ResponseEntity.ok(toFoodsharingConnectionStatus(
                foodsharingPickupAutomationService.connect(request.getEmail(), request.getPassword())));
    }

    @Override
    public ResponseEntity<Void> disconnectFoodsharing() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATIONS);
        foodsharingPickupAutomationService.disconnect();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FoodsharingConnectionStatus> getFoodsharingStatus() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATIONS);
        return ResponseEntity.ok(toFoodsharingConnectionStatus(foodsharingPickupAutomationService.status()));
    }

    @Override
    public ResponseEntity<FoodsharingStoreAutomationOverview> getFoodsharingStores() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATIONS);
        FoodsharingPickupAutomationService.StoreOverviewView overview = foodsharingPickupAutomationService.stores();
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
    public ResponseEntity<FoodsharingStoreAutomation> saveFoodsharingStoreAutomation(Long storeId, FoodsharingStoreAutomationRequest request) {
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
                foodsharingPickupAutomationService.save(storeId, serviceRequest)));
    }

    @Override
    public ResponseEntity<FoodsharingRunResult> runFoodsharingAutomation(FoodsharingRunRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATIONS);
        return ResponseEntity.ok(toFoodsharingRunResult(
                foodsharingPickupAutomationService.run(Boolean.TRUE.equals(request.getDryRun()))));
    }


    @Override
    public ResponseEntity<List<FoodsharingCleaningRuleExemption>> getFoodsharingCleaningRuleExemptions() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        return ResponseEntity.ok(foodsharingPickupAutomationService.cleaningRuleExemptions().stream()
                .map(this::toFoodsharingCleaningRuleExemption)
                .toList());
    }

    @Override
    public ResponseEntity<FoodsharingCleaningRuleExemption> saveFoodsharingCleaningRuleExemption(FoodsharingCleaningRuleExemptionRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        FoodsharingPickupAutomationService.CleaningRuleExemptionRequest serviceRequest =
                new FoodsharingPickupAutomationService.CleaningRuleExemptionRequest(request.getFoodsharingId(), request.getReason());
        return ResponseEntity.ok(toFoodsharingCleaningRuleExemption(foodsharingPickupAutomationService.saveCleaningRuleExemption(serviceRequest)));
    }

    @Override
    public ResponseEntity<Void> deleteFoodsharingCleaningRuleExemption(UUID exemptionId) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
        foodsharingPickupAutomationService.deleteCleaningRuleExemption(exemptionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<FoodsharingAutomationAudit>> getFoodsharingAutomationAudit() {
        currentActorService.requirePermission(UserPermission.CAN_SEE_ALL_AUTOMATION_DECISIONS);
        return ResponseEntity.ok(foodsharingPickupAutomationService.audit().stream()
                .map(this::toFoodsharingAutomationAudit)
                .toList());
    }

    @Override
    public ResponseEntity<List<FoodsharingFuturePickupUser>> getFoodsharingFuturePickupUsers() {
        currentActorService.requirePermission(UserPermission.CAN_SEE_USER_PICKUP_COUNT_GROUPING);
        return ResponseEntity.ok(foodsharingPickupAutomationService.futurePickupUsers().stream()
                .map(this::toFoodsharingFuturePickupUser)
                .toList());
    }

    private FoodsharingConnectionStatus toFoodsharingConnectionStatus(FoodsharingPickupAutomationService.ConnectionStatus status) {
        FoodsharingConnectionStatus response = new FoodsharingConnectionStatus();
        response.setConnected(status.connected());
        response.setEmail(status.email());
        response.setFoodsharingUserId(status.foodsharingUserId());
        response.setAuthenticatedAt(toOffsetDateTime(status.authenticatedAt()));
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
