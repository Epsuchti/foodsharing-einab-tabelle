package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public Page<Slot> getBookingsByFoodsharingId(String foodsharingId, int page, int size) {
        BookingUser bookingUser = bookingUserService.getByFoodsharingId(foodsharingId);
        return slotRepository.findAllByBookingUserAndStatuses(
                bookingUser,
                Set.of(SlotStatus.BOOKED, SlotStatus.DONE),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    public BookingUser getProfileByFoodsharingId(String foodsharingId) {
        return bookingUserService.getByFoodsharingId(foodsharingId);
    }

    @Transactional
    public Slot cancelBooking(String foodsharingId, UUID slotId) {
        Slot slot = slotRepository.findForUpdateById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_NOT_FOUND));
        if (slot.getBookingUser() == null || !foodsharingId.equalsIgnoreCase(slot.getBookingUser().getFoodsharingId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ONLY_OWN_BOOKINGS_CANCELLABLE);
        }
        if (slot.getStatus() != SlotStatus.BOOKED) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.ONLY_BOOKED_APPOINTMENTS_CANCELLABLE);
        }
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setBookingUser(null);
        slot.setBookedAt(null);
        slot.setDoneAt(null);
        return slot;
    }
}
