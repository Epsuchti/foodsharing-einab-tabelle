package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.AdminApi;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.service.AdminService;
import ch.it4user.foodsharing.service.FoodsharingPickupAutomationService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/api/admin/foodsharing/connect")
    public FoodsharingPickupAutomationService.ConnectionStatus connectFoodsharing(@RequestBody FoodsharingConnectRequest request) {
        return foodsharingPickupAutomationService.connect(request.email(), request.password());
    }

    @DeleteMapping("/api/admin/foodsharing/connect")
    public ResponseEntity<Void> disconnectFoodsharing() {
        foodsharingPickupAutomationService.disconnect();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/foodsharing/status")
    public FoodsharingPickupAutomationService.ConnectionStatus getFoodsharingStatus() {
        return foodsharingPickupAutomationService.status();
    }

    @GetMapping("/api/admin/foodsharing/stores")
    public java.util.List<FoodsharingPickupAutomationService.StoreAutomationView> getFoodsharingStores() {
        return foodsharingPickupAutomationService.stores();
    }

    @PutMapping("/api/admin/foodsharing/stores/{storeId}/automation")
    public FoodsharingPickupAutomationService.StoreAutomationView saveFoodsharingStoreAutomation(@PathVariable long storeId, @RequestBody FoodsharingPickupAutomationService.StoreAutomationRequest request) {
        return foodsharingPickupAutomationService.save(storeId, request);
    }

    @PostMapping("/api/admin/foodsharing/automation/run")
    public ch.it4user.foodsharing.service.FoodsharingPickupModels.RunResult runFoodsharingAutomation(@RequestBody FoodsharingRunRequest request) {
        return foodsharingPickupAutomationService.run(request.dryRun());
    }

    @GetMapping("/api/admin/foodsharing/automation/audit")
    public java.util.List<FoodsharingPickupAutomationService.AuditView> getFoodsharingAutomationAudit() {
        return foodsharingPickupAutomationService.audit();
    }

    @GetMapping("/api/admin/foodsharing/future-pickup-users")
    public java.util.List<FoodsharingPickupAutomationService.StorePickupUserView> getFoodsharingFuturePickupUsers() {
        return foodsharingPickupAutomationService.futurePickupUsers();
    }

    public record FoodsharingConnectRequest(String email, String password) {}
    public record FoodsharingRunRequest(boolean dryRun) {}

}
