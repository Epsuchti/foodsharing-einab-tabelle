package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Teacher", slot.getEinAb().getTeacher().getName());
        details.put("Category", slot.getEinAb().getCategory().name());
        details.put("Start", emailTemplateService.swissDateTime(slot.getEinAb().getStartDateTime()));
        details.put("Location", valueOrDash(slot.getEinAb().getLocation()));
        details.put("What to bring", valueOrDash(slot.getEinAb().getWhatToBring()));
        details.put("Fairteiler visit", slot.getEinAb().isVisitFairteiler() ? "yes" : "no");
        String body = emailTemplateService.render(
                "Your pickup is booked",
                emailTemplateService.paragraph("Hello " + slot.getBookingUser().getName() + ",")
                        + emailTemplateService.paragraph("Your pickup has been booked successfully.")
                        + emailTemplateService.detailsTable(details)
                        + emailTemplateService.note("You can log in later with the email address you used here: "
                        + slot.getBookingUser().getEmail())
                        + emailTemplateService.button("View your bookings", manageUrl)
        );
        emailService.send(slot.getBookingUser().getEmail(), "Your foodsharing pickup details", body);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
