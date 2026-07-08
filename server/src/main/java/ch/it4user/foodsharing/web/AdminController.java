package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.AdminApi;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.FoodsharingAutomationAudit;
import ch.it4user.foodsharing.openapi.model.FoodsharingConnectRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingConnectionStatus;
import ch.it4user.foodsharing.openapi.model.FoodsharingFuturePickup;
import ch.it4user.foodsharing.openapi.model.FoodsharingFuturePickupUser;
import ch.it4user.foodsharing.openapi.model.FoodsharingRunRequest;
import ch.it4user.foodsharing.openapi.model.FoodsharingRunResult;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.openapi.model.FoodsharingStoreAutomationRequest;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.service.AdminService;
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

    public AdminController(AdminService adminService, ApiModelMapper mapper, FoodsharingPickupAutomationService foodsharingPickupAutomationService) {
        this.adminService = adminService;
        this.mapper = mapper;
        this.foodsharingPickupAutomationService = foodsharingPickupAutomationService;
    }

    @Override
    public ResponseEntity<TeacherListResponse> getAdminTeachers(Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toTeacherListResponse(adminService.getTeachers(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<TeacherListResponse> getAdminAdmins(Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toTeacherListResponse(adminService.getAdmins(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<TeacherResponse> enableAdminTeacher(UUID teacherId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherActive(teacherId, true)));
    }

    @Override
    public ResponseEntity<TeacherResponse> disableAdminTeacher(UUID teacherId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherActive(teacherId, false)));
    }

    @Override
    public ResponseEntity<TeacherResponse> grantAdminTeacher(UUID teacherId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherAdmin(teacherId, true)));
    }

    @Override
    public ResponseEntity<TeacherResponse> revokeAdminTeacher(UUID teacherId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setTeacherAdmin(teacherId, false)));
    }

    @Override
    public ResponseEntity<AdminEinAbListResponse> getAdminEinAbs(Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toAdminEinAbListResponse(adminService.getEinAbs(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<BookingListResponse> getAdminBookings(Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toBookingListResponse(adminService.getBookings(page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<AdminBookingUserPageResponse> getAdminUsers(Integer page, Integer size, Boolean threePickupsOnly) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 50 : size;
        boolean resolvedThreePickupsOnly = threePickupsOnly != null && threePickupsOnly;
        return ResponseEntity.ok(mapper.toAdminBookingUserPageResponse(adminService.getUsers(resolvedPage, resolvedSize, resolvedThreePickupsOnly)));
    }

    @Override
    public ResponseEntity<BookingUserResponse> disableAdminBookingUser(UUID bookingUserId) {
        return ResponseEntity.ok(mapper.toBookingUserResponse(adminService.disableBookingUser(bookingUserId)));
    }

    @Override
    public ResponseEntity<BookingUserResponse> grantAdminBookingUser(UUID bookingUserId) {
        return ResponseEntity.ok(mapper.toBookingUserResponse(adminService.grantBookingUserAdmin(bookingUserId)));
    }

    @Override
    public ResponseEntity<TeacherResponse> enableAdminUser(UUID adminUserId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setAdminActive(adminUserId, true)));
    }

    @Override
    public ResponseEntity<TeacherResponse> disableAdminUser(UUID adminUserId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setAdminActive(adminUserId, false)));
    }

    @Override
    public ResponseEntity<TeacherResponse> revokeAdminUser(UUID adminUserId) {
        return ResponseEntity.ok(mapper.toTeacherResponse(adminService.setAdminAdmin(adminUserId, false)));
    }

    @Override
    public ResponseEntity<FoodsharingConnectionStatus> connectFoodsharing(FoodsharingConnectRequest request) {
        return ResponseEntity.ok(toFoodsharingConnectionStatus(
                foodsharingPickupAutomationService.connect(request.getEmail(), request.getPassword())));
    }

    @Override
    public ResponseEntity<Void> disconnectFoodsharing() {
        foodsharingPickupAutomationService.disconnect();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FoodsharingConnectionStatus> getFoodsharingStatus() {
        return ResponseEntity.ok(toFoodsharingConnectionStatus(foodsharingPickupAutomationService.status()));
    }

    @Override
    public ResponseEntity<List<FoodsharingStoreAutomation>> getFoodsharingStores() {
        return ResponseEntity.ok(foodsharingPickupAutomationService.stores().stream()
                .map(this::toFoodsharingStoreAutomation)
                .toList());
    }

    @Override
    public ResponseEntity<FoodsharingStoreAutomation> saveFoodsharingStoreAutomation(Long storeId, FoodsharingStoreAutomationRequest request) {
        FoodsharingPickupAutomationService.StoreAutomationRequest serviceRequest =
                new FoodsharingPickupAutomationService.StoreAutomationRequest(
                        request.getStoreName(),
                        Boolean.TRUE.equals(request.getEnabled()),
                        Boolean.TRUE.equals(request.getGapRuleEnabled()),
                        request.getMinimumGapDays() == null ? 0 : request.getMinimumGapDays(),
                        Boolean.TRUE.equals(request.getCleaningRuleEnabled()),
                        Boolean.TRUE.equals(request.getExperienceRuleEnabled()));
        return ResponseEntity.ok(toFoodsharingStoreAutomation(
                foodsharingPickupAutomationService.save(storeId, serviceRequest)));
    }

    @Override
    public ResponseEntity<FoodsharingRunResult> runFoodsharingAutomation(FoodsharingRunRequest request) {
        return ResponseEntity.ok(toFoodsharingRunResult(
                foodsharingPickupAutomationService.run(Boolean.TRUE.equals(request.getDryRun()))));
    }

    @Override
    public ResponseEntity<List<FoodsharingAutomationAudit>> getFoodsharingAutomationAudit() {
        return ResponseEntity.ok(foodsharingPickupAutomationService.audit().stream()
                .map(this::toFoodsharingAutomationAudit)
                .toList());
    }

    @Override
    public ResponseEntity<List<FoodsharingFuturePickupUser>> getFoodsharingFuturePickupUsers() {
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
        return response;
    }

    private FoodsharingStoreAutomation toFoodsharingStoreAutomation(FoodsharingPickupAutomationService.StoreAutomationView store) {
        FoodsharingStoreAutomation response = new FoodsharingStoreAutomation();
        response.setStoreId(store.storeId());
        response.setStoreName(store.storeName());
        response.setEnabled(store.enabled());
        response.setGapRuleEnabled(store.gapRuleEnabled());
        response.setMinimumGapDays(store.minimumGapDays());
        response.setCleaningRuleEnabled(store.cleaningRuleEnabled());
        response.setExperienceRuleEnabled(store.experienceRuleEnabled());
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
