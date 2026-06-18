package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.AdminApi;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.service.AdminService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController implements AdminApi {

    private final AdminService adminService;
    private final ApiModelMapper mapper;

    public AdminController(AdminService adminService, ApiModelMapper mapper) {
        this.adminService = adminService;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<TeacherListResponse> getAdminTeachers() {
        return ResponseEntity.ok(mapper.toTeacherListResponse(adminService.getTeachers()));
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
    public ResponseEntity<AdminEinAbListResponse> getAdminEinAbs() {
        return ResponseEntity.ok(mapper.toAdminEinAbListResponse(adminService.getEinAbs()));
    }

    @Override
    public ResponseEntity<BookingListResponse> getAdminBookings() {
        return ResponseEntity.ok(mapper.toBookingListResponse(adminService.getBookings()));
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
}
