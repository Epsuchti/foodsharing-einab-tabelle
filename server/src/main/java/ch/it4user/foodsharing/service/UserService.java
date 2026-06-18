package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final BookingUserService bookingUserService;
    private final SlotRepository slotRepository;

    public UserService(BookingUserService bookingUserService, SlotRepository slotRepository) {
        this.bookingUserService = bookingUserService;
        this.slotRepository = slotRepository;
    }

    public List<Slot> getBookingsByEmail(String email) {
        List<BookingUser> users = bookingUserService.findByEmail(email);
        if (users.isEmpty()) {
            return List.of();
        }
        return slotRepository.findAllByBookingUsersAndStatuses(users, Set.of(SlotStatus.BOOKED, SlotStatus.DONE));
    }

    @Transactional
    public Slot cancelBooking(String email, UUID slotId) {
        Slot slot = slotRepository.findForUpdateById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (slot.getBookingUser() == null || !email.equalsIgnoreCase(slot.getBookingUser().getEmail())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own bookings");
        }
        if (slot.getStatus() != SlotStatus.BOOKED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only booked appointments can be cancelled");
        }
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setBookingUser(null);
        slot.setBookedAt(null);
        slot.setDoneAt(null);
        return slot;
    }
}
