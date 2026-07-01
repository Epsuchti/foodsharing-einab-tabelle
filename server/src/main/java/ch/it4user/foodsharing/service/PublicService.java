package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicService {

    private static final Set<SlotStatus> ACTIVE_BOOKING_STATUSES = Set.of(SlotStatus.BOOKED, SlotStatus.DONE);

    private final SlotRepository slotRepository;
    private final BookingUserService bookingUserService;
    private final TeacherRepository teacherRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final AppProperties appProperties;

    public PublicService(SlotRepository slotRepository,
                         BookingUserService bookingUserService,
                         TeacherRepository teacherRepository,
                         EmailService emailService,
                         EmailTemplateService emailTemplateService,
                         AppProperties appProperties) {
        this.slotRepository = slotRepository;
        this.bookingUserService = bookingUserService;
        this.teacherRepository = teacherRepository;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.appProperties = appProperties;
    }

    public Page<Slot> findAvailableSlots(String search, EinAbCategory category, Boolean visitFairteiler, int page, int size) {
        return slotRepository.findAvailableSlots(
                normalizeSearch(search),
                category,
                visitFairteiler,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    @Transactional
    public Slot bookSlot(UUID slotId, String email, String name, String foodsharingId, String phoneNumber, LanguageCode language) {
        String normalizedEmail = email.trim().toLowerCase();
        if (teacherRepository.existsByFoodsharingIdIgnoreCase(foodsharingId.trim())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.TEACHERS_CANNOT_BOOK);
        }
        Slot slot = slotRepository.findForUpdateById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.SLOT_NOT_FOUND));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.SLOT_NOT_AVAILABLE);
        }

        BookingUser bookingUser = bookingUserService.getOrCreate(normalizedEmail, name, foodsharingId, phoneNumber, language);
        if (slotRepository.existsByBookingUserAndStatusInAndEinAbTeacher(
                bookingUser, ACTIVE_BOOKING_STATUSES, slot.getEinAb().getTeacher())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_ALREADY_BOOKED_WITH_TEACHER);
        }
        if (slotRepository.existsByBookingUserAndStatusInAndEinAbCategory(
                bookingUser, ACTIVE_BOOKING_STATUSES, slot.getEinAb().getCategory())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_ALREADY_BOOKED_IN_CATEGORY);
        }
        if (slotRepository.countByBookingUserAndStatusIn(bookingUser, ACTIVE_BOOKING_STATUSES) >= 3) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_BOOKING_LIMIT_REACHED);
        }
        Integer minimumPickupCount = slot.getEinAb().getMinimumPickupCount();
        if (minimumPickupCount != null
                && slotRepository.countByBookingUserAndStatus(bookingUser, SlotStatus.DONE) < minimumPickupCount) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.MINIMUM_PICKUP_COUNT_NOT_REACHED);
        }

        slot.setBookingUser(bookingUser);
        slot.setStatus(SlotStatus.BOOKED);
        slot.setBookedAt(java.time.Instant.now());
        sendBookingConfirmationEmail(slot);
        return slot;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSearch(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }

    private void sendBookingConfirmationEmail(Slot slot) {
        String manageUrl = appProperties.getFrontend().getBaseUrl() + "/my-bookings";
        LanguageCode language = slot.getBookingUser().getPreferredLanguage();
        emailService.send(
                slot.getBookingUser().getEmail(),
                emailTemplateService.bookingConfirmationSubject(language),
                emailTemplateService.bookingConfirmationBody(language, slot, manageUrl));
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
