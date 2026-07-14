package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.User;
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
    private final BezirkService bezirkService;
    private final SlotRepository slotRepository;

    public UserService(BookingUserService bookingUserService,
                       BezirkService bezirkService,
                       SlotRepository slotRepository) {
        this.bookingUserService = bookingUserService;
        this.bezirkService = bezirkService;
        this.slotRepository = slotRepository;
    }

    public Page<Slot> getBookingsByFoodsharingId(String bezirkSlug, String foodsharingId, int page, int size) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        User bookingUser = bookingUserService.getByFoodsharingId(foodsharingId);
        ensureUserCanUseBezirk(bookingUser, bezirk);
        return slotRepository.findAllByBookingUserAndStatusesAndBezirk(
                bookingUser,
                Set.of(SlotStatus.BOOKED, SlotStatus.DONE),
                bezirk,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    public User getProfileByFoodsharingId(String bezirkSlug, String foodsharingId) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        User bookingUser = bookingUserService.getByFoodsharingId(foodsharingId);
        ensureUserCanUseBezirk(bookingUser, bezirk);
        return bookingUser;
    }

    public User getProfileByFoodsharingId(String foodsharingId) {
        return bookingUserService.getByFoodsharingId(foodsharingId);
    }

    @Transactional
    public Slot cancelBooking(String bezirkSlug, String foodsharingId, UUID slotId) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        User bookingUser = bookingUserService.getByFoodsharingId(foodsharingId);
        ensureUserCanUseBezirk(bookingUser, bezirk);
        Slot slot = slotRepository.findForUpdateByIdAndBezirk(slotId, bezirk)
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

    private void ensureUserCanUseBezirk(User bookingUser, Bezirk bezirk) {
        if (bookingUser.getBezirk() != null && !bookingUser.getBezirk().getId().equals(bezirk.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_BEZIRK_MISMATCH);
        }
    }
}
