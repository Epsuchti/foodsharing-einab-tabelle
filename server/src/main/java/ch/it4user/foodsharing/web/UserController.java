package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.openapi.api.UserApi;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import java.util.UUID;
import ch.it4user.foodsharing.service.CurrentActorService;
import ch.it4user.foodsharing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApi {

    private final UserService userService;
    private final CurrentActorService currentActorService;
    private final ApiModelMapper mapper;

    public UserController(UserService userService, CurrentActorService currentActorService, ApiModelMapper mapper) {
        this.userService = userService;
        this.currentActorService = currentActorService;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<BookingListResponse> getMyBookings() {
        return ResponseEntity.ok(mapper.toBookingListResponse(
                userService.getBookingsByEmail(currentActorService.requireEmail())));
    }

    @Override
    public ResponseEntity<BookingUserResponse> getMyProfile() {
        BookingUser bookingUser = userService.getProfileByEmail(currentActorService.requireEmail());
        return ResponseEntity.ok(mapper.toBookingUserResponse(bookingUser));
    }

    @Override
    public ResponseEntity<Void> cancelMyBooking(UUID slotId) {
        userService.cancelBooking(currentActorService.requireEmail(), slotId);
        return ResponseEntity.noContent().build();
    }
}
