package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.domain.entity.User;
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
    public ResponseEntity<BookingListResponse> getMyBookings(Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toBookingListResponse(
                userService.getBookingsByFoodsharingId(currentActorService.requireFoodsharingId(), page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<BookingUserResponse> getMyProfile() {
        User bookingUser = userService.getProfileByFoodsharingId(currentActorService.requireFoodsharingId());
        return ResponseEntity.ok(mapper.toBookingUserResponse(bookingUser));
    }

    @Override
    public ResponseEntity<Void> cancelMyBooking(UUID slotId) {
        userService.cancelBooking(currentActorService.requireFoodsharingId(), slotId);
        return ResponseEntity.noContent().build();
    }
}
