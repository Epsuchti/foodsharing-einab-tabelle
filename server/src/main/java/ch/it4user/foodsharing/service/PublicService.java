package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicService {

    private static final Set<SlotStatus> ACTIVE_BOOKING_STATUSES = Set.of(SlotStatus.BOOKED, SlotStatus.DONE);

    private final SlotRepository slotRepository;
    private final BookingUserService bookingUserService;
    private final TeacherRepository teacherRepository;

    public PublicService(SlotRepository slotRepository, BookingUserService bookingUserService, TeacherRepository teacherRepository) {
        this.slotRepository = slotRepository;
        this.bookingUserService = bookingUserService;
        this.teacherRepository = teacherRepository;
    }

    public List<Slot> findAvailableSlots(String search, EinAbCategory category, Boolean visitFairteiler) {
        return slotRepository.findAvailableSlots(normalizeSearch(search), category, visitFairteiler);
    }

    @Transactional
    public Slot bookSlot(UUID slotId, String email, String name, String foodsharingId, String phoneNumber) {
        String normalizedEmail = email.trim().toLowerCase();
        if (teacherRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Teachers cannot book appointments");
        }
        Slot slot = slotRepository.findForUpdateById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "Slot is no longer available");
        }

        BookingUser bookingUser = bookingUserService.getOrCreate(normalizedEmail, name, foodsharingId, phoneNumber);
        if (slotRepository.existsByBookingUserAndStatusInAndEinAbTeacher(
                bookingUser, ACTIVE_BOOKING_STATUSES, slot.getEinAb().getTeacher())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an appointment with this teacher");
        }
        if (slotRepository.existsByBookingUserAndStatusInAndEinAbCategory(
                bookingUser, ACTIVE_BOOKING_STATUSES, slot.getEinAb().getCategory())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an appointment in this category");
        }
        if (slotRepository.countByBookingUserAndStatusIn(bookingUser, ACTIVE_BOOKING_STATUSES) >= 3) {
            throw new ApiException(HttpStatus.CONFLICT, "You can only book up to 3 appointments");
        }

        slot.setBookingUser(bookingUser);
        slot.setStatus(SlotStatus.BOOKED);
        slot.setBookedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return slot;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSearch(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }
}
