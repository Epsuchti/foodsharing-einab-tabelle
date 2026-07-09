package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
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
public class PublicService {

    private static final Set<SlotStatus> ACTIVE_BOOKING_STATUSES = Set.of(SlotStatus.PENDING_CONFIRMATION, SlotStatus.BOOKED, SlotStatus.DONE);

    private final SlotRepository slotRepository;
    private final BookingUserService bookingUserService;
    private final FoodsharingMessageService messageService;
    private final MessageTemplateService messageTemplateService;
    private final TokenService tokenService;
    private final AppProperties appProperties;

    public PublicService(SlotRepository slotRepository,
                         BookingUserService bookingUserService,
                         FoodsharingMessageService messageService,
                         MessageTemplateService messageTemplateService,
                         TokenService tokenService,
                         AppProperties appProperties) {
        this.slotRepository = slotRepository;
        this.bookingUserService = bookingUserService;
        this.messageService = messageService;
        this.messageTemplateService = messageTemplateService;
        this.tokenService = tokenService;
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
    public Slot bookSlot(UUID slotId, String foodsharingId, LanguageCode language) {
        Slot slot = slotRepository.findForUpdateById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.SLOT_NOT_FOUND));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.SLOT_NOT_AVAILABLE);
        }

        User bookingUser = bookingUserService.getOrCreate(foodsharingId, language);
        if (slotRepository.existsByBookingUserAndStatusInAndEinAbTeacher(
                bookingUser, ACTIVE_BOOKING_STATUSES, slot.getEinAb().getTeacher())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_ALREADY_BOOKED_WITH_TEACHER);
        }
        if (slotRepository.existsByBookingUserAndStatusInAndEinAbCategory(
                bookingUser, ACTIVE_BOOKING_STATUSES, slot.getEinAb().getCategory())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_ALREADY_BOOKED_IN_CATEGORY);
        }
        Integer minimumPickupCount = slot.getEinAb().getMinimumPickupCount();
        if (minimumPickupCount != null
                && slotRepository.countByBookingUserAndStatus(bookingUser, SlotStatus.DONE) < minimumPickupCount) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.MINIMUM_PICKUP_COUNT_NOT_REACHED);
        }

        slot.setBookingUser(bookingUser);
        String rawToken = tokenService.generateToken();
        slot.setStatus(SlotStatus.PENDING_CONFIRMATION);
        slot.setBookedAt(java.time.Instant.now());
        slot.setPendingConfirmationTokenHash(tokenService.hash(rawToken));
        slot.setPendingConfirmationExpiresAt(java.time.Instant.now().plus(appProperties.getAuth().getBookingConfirmationValidityMinutes(), java.time.temporal.ChronoUnit.MINUTES));
        sendBookingConfirmationMessage(slot, rawToken, appProperties.getAuth().getBookingConfirmationValidityMinutes());
        return slot;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSearch(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }

    private void sendBookingConfirmationMessage(Slot slot, String rawToken, long confirmWithinMinutes) {
        String confirmUrl = appProperties.getFrontend().getBaseUrl() + "/confirm-booking?token=" + rawToken;
        LanguageCode language = slot.getBookingUser().getPreferredLanguage();
        messageService.send(
                slot.getBookingUser().getFoodsharingId(),
                messageTemplateService.bookingConfirmationSubject(language),
                messageTemplateService.bookingConfirmationBody(language, slot, confirmUrl, confirmWithinMinutes));
    }

    @Transactional
    public Slot confirmBooking(String token) {
        Slot slot = slotRepository.findByPendingConfirmationTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_BOOKING_CONFIRMATION_TOKEN));
        if (slot.getPendingConfirmationExpiresAt() == null || slot.getPendingConfirmationExpiresAt().isBefore(java.time.Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.BOOKING_CONFIRMATION_EXPIRED);
        }
        slot.setStatus(SlotStatus.BOOKED);
        slot.setPendingConfirmationTokenHash(null);
        slot.setPendingConfirmationExpiresAt(null);
        return slot;
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
