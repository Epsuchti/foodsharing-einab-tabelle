package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.EinAbRepository;
import ch.it4user.foodsharing.repository.BookingCommentRepository;
import ch.it4user.foodsharing.service.EinAbCreatedEvent;
import ch.it4user.foodsharing.repository.UserRepository;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private static final Set<SlotStatus> BLOCKING_STATUSES = Set.of(SlotStatus.PENDING_CONFIRMATION, SlotStatus.BOOKED, SlotStatus.DONE);

    private final UserRepository userRepository;
    private final EinAbRepository einAbRepository;
    private final SlotRepository slotRepository;
    private final BookingCommentRepository bookingCommentRepository;
    private final IcalImportService icalImportService;
    private final BezirkService bezirkService;
    private final BookingUserService bookingUserService;
    private final ApplicationEventPublisher eventPublisher;

    public TeacherService(UserRepository userRepository,
                          EinAbRepository einAbRepository,
                          SlotRepository slotRepository,
                          BookingCommentRepository bookingCommentRepository,
                          IcalImportService icalImportService,
                          BezirkService bezirkService,
                          BookingUserService bookingUserService,
                          ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.einAbRepository = einAbRepository;
        this.slotRepository = slotRepository;
        this.bookingCommentRepository = bookingCommentRepository;
        this.icalImportService = icalImportService;
        this.bezirkService = bezirkService;
        this.bookingUserService = bookingUserService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public User signup(String bezirkSlug, String foodsharingId, String icalLink, LanguageCode language) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        User teacher = bookingUserService.getOrCreate(foodsharingId, language);
        if (!teacher.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.TEACHER_INACTIVE);
        }
        bookingUserService.assignToBezirk(teacher, bezirk);
        teacher.setWantsToBeTeacher(true);
        teacher.setActive(true);
        teacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        return teacher;
    }

    @Transactional
    public User updateProfile(String bezirkSlug, User teacher, String phoneNumber, String icalLink, LanguageCode language) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        return updateProfile(teacher, phoneNumber, icalLink, language);
    }

    @Transactional
    public User updateProfile(User teacher, String phoneNumber, String icalLink, LanguageCode language) {
        User managedTeacher = userRepository.findWithBezirkById(teacher.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        managedTeacher.setPhoneNumber(phoneNumber == null ? null : phoneNumber.trim());
        managedTeacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        managedTeacher.setPreferredLanguage(language);
        return managedTeacher;
    }

    @Transactional
    public User assignBezirk(User teacher, String bezirkSlug) {
        User managedTeacher = userRepository.findWithBezirkById(teacher.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        if (managedTeacher.getBezirk() != null) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_BEZIRK_MISMATCH);
        }
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        bookingUserService.assignToBezirk(managedTeacher, bezirk);
        return managedTeacher;
    }

    public Page<EinAb> findTeacherEinAbs(String bezirkSlug, User teacher, int page, int size) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        return einAbRepository.findAllByTeacherAndBezirkOrderByStartDateTimeAsc(
                teacher,
                bezirk,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public Page<Slot> findTeacherBookings(String bezirkSlug, User teacher, int page, int size) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        return slotRepository.findAllByTeacherAndStatusesAndBezirk(
                teacher,
                Set.of(SlotStatus.PENDING_CONFIRMATION, SlotStatus.BOOKED, SlotStatus.DONE),
                bezirk,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public List<BookingComment> findBookingComments(String bezirkSlug, User teacher, UUID bookingUserId) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        User bookingUser = requireBookingUser(bookingUserId, bezirk);
        if (!bookingUser.isActive()) {
            return List.of();
        }
        return bookingCommentRepository.findAllByBookingUserOrderByCreatedAtAsc(bookingUser);
    }

    @Transactional
    public BookingComment addBookingComment(String bezirkSlug, User teacher, UUID bookingUserId, String comment) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        User bookingUser = requireBookingUser(bookingUserId, bezirk);
        if (!bookingUser.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.BOOKING_USER_DISABLED);
        }
        BookingComment bookingComment = new BookingComment();
        bookingComment.setBookingUser(bookingUser);
        bookingComment.setTeacher(teacher);
        bookingComment.setComment(comment.trim());
        return bookingCommentRepository.save(bookingComment);
    }

    public List<ch.it4user.foodsharing.openapi.model.IcalCandidate> getIcalCandidates(String bezirkSlug, User teacher) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        return getIcalCandidates(teacher);
    }

    public List<ch.it4user.foodsharing.openapi.model.IcalCandidate> getIcalCandidates(User teacher) {
        return icalImportService.loadCandidates(teacher.getIcalLink());
    }

    @Transactional
    public EinAb createEinAb(String bezirkSlug,
                             User teacher,
                             EinAbCategory category,
                             Instant startDateTime,
                             String location,
                             String publicLocation,
                             String onlineCallLink,
                             String whatToBring,
                             String hint,
                             boolean visitFairteiler,
                             int slotCount,
                             Integer minimumPickupCount) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        ensureTeacherBezirk(teacher, bezirk);
        ensureTeacherActive(teacher);
        validateEinAb(category, slotCount, publicLocation, onlineCallLink, minimumPickupCount);
        EinAb einAb = new EinAb();
        einAb.setBezirk(bezirk);
        einAb.setTeacher(teacher);
        einAb.setCategory(category);
        einAb.setStartDateTime(startDateTime);
        einAb.setLocation(isOnline(category) ? null : normalizeLocation(location));
        einAb.setPublicLocation(isOnline(category) ? null : normalizeRequiredLocation(publicLocation));
        einAb.setOnlineCallLink(isOnline(category) ? normalizeRequiredOnlineCallLink(onlineCallLink) : null);
        einAb.setWhatToBring(isOnline(category) ? null : normalizeWhatToBring(whatToBring));
        einAb.setHint(normalizeWhatToBring(hint));
        einAb.setVisitFairteiler(isOnline(category) ? false : visitFairteiler);
        einAb.setSlotCount(isOnline(category) ? 1 : slotCount);
        einAb.setMinimumPickupCount(isOnline(category) ? null : normalizeMinimumPickupCount(minimumPickupCount));
        EinAb savedEinAb = einAbRepository.save(einAb);
        createSlots(savedEinAb, isOnline(category) ? 1 : slotCount);
        EinAb reloadedEinAb = reloadEinAbWithTeacher(savedEinAb.getId(), bezirk);
        eventPublisher.publishEvent(new EinAbCreatedEvent(reloadedEinAb.getId()));
        return reloadedEinAb;
    }

    @Transactional
    public EinAb updateEinAb(String bezirkSlug,
                             User teacher,
                             UUID einAbId,
                             EinAbCategory category,
                             Instant startDateTime,
                             String location,
                             String publicLocation,
                             String onlineCallLink,
                             String whatToBring,
                             String hint,
                             boolean visitFairteiler,
                             int slotCount,
                             Integer minimumPickupCount,
                             boolean admin) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        validateEinAb(category, slotCount, publicLocation, onlineCallLink, minimumPickupCount);
        EinAb einAb = requireTeacherEinAb(teacher, einAbId, admin, bezirk);
        if (!admin) {
            ensureTeacherBezirk(teacher, bezirk);
            ensureTeacherActive(teacher);
        }
        int existingSlots = slotRepository.findAllByEinAbOrderByCreatedAtAsc(einAb).size();
        if (!isOnline(category) && slotCount < existingSlots) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.SLOT_COUNT_REDUCTION_NOT_SUPPORTED);
        }

        einAb.setCategory(category);
        einAb.setStartDateTime(startDateTime);
        einAb.setLocation(isOnline(category) ? null : normalizeLocation(location));
        einAb.setPublicLocation(isOnline(category) ? null : normalizeRequiredLocation(publicLocation));
        einAb.setOnlineCallLink(isOnline(category) ? normalizeRequiredOnlineCallLink(onlineCallLink) : null);
        einAb.setWhatToBring(isOnline(category) ? null : normalizeWhatToBring(whatToBring));
        einAb.setHint(normalizeWhatToBring(hint));
        einAb.setVisitFairteiler(isOnline(category) ? false : visitFairteiler);
        einAb.setSlotCount(isOnline(category) ? 1 : slotCount);
        einAb.setMinimumPickupCount(isOnline(category) ? null : normalizeMinimumPickupCount(minimumPickupCount));
        if (!isOnline(category) && slotCount > existingSlots) {
            createSlots(einAb, slotCount - existingSlots);
        }
        return reloadEinAbWithTeacher(einAb.getId(), bezirk);
    }

    @Transactional
    public void deleteEinAb(String bezirkSlug, User teacher, UUID einAbId, boolean admin) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        if (!admin) {
            ensureTeacherBezirk(teacher, bezirk);
        }
        EinAb einAb = requireTeacherEinAb(teacher, einAbId, admin, bezirk);
        if (slotRepository.existsByEinAbAndStatusIn(einAb, BLOCKING_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.BOOKED_SLOTS_PREVENT_DELETE);
        }
        List<Slot> slots = slotRepository.findAllByEinAbOrderByCreatedAtAsc(einAb);
        slotRepository.deleteAll(slots);
        einAbRepository.delete(einAb);
    }

    public Page<EinAb> findAllEinAbs(String bezirkSlug, int page, int size) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        return einAbRepository.findAllByBezirkOrderByStartDateTimeAsc(
                bezirk,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public Page<User> findAllTeachers(String bezirkSlug, int page, int size) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        return userRepository.findAllTeachersByBezirk(
                bezirk,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    @Transactional
    public User setTeacherActive(UUID teacherId, boolean active) {
        User teacher = userRepository.findWithBezirkById(teacherId)
                .filter(user -> user.isCanGiveEinAbs() || user.isWantsToBeTeacher())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        teacher.setCanGiveEinAbs(active);
        teacher.setWantsToBeTeacher(false);
        if (active) {
            teacher.setActive(true);
        }
        return teacher;
    }

    @Transactional
    public User setTeacherAdmin(UUID teacherId, boolean admin) {
        User teacher = userRepository.findWithBezirkById(teacherId)
                .filter(user -> user.isCanGiveEinAbs() || user.isWantsToBeTeacher())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        teacher.setCanManageUsers(admin);
        if (admin) {
            teacher.setActive(true);
            teacher.setCanGiveEinAbs(true);
            teacher.setWantsToBeTeacher(false);
        }
        return teacher;
    }

    @Transactional
    public Slot cancelBookedSlot(String bezirkSlug, User teacher, UUID slotId, boolean admin) {
        Bezirk bezirk = bezirkService.requireActive(bezirkSlug);
        if (!admin) {
            ensureTeacherBezirk(teacher, bezirk);
        }
        Slot slot = slotRepository.findForUpdateByIdAndBezirk(slotId, bezirk)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.SLOT_NOT_FOUND));
        if (!admin && !slot.getEinAb().getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ONLY_OWN_EINABS_MANAGEABLE);
        }
        if (slot.getStatus() != SlotStatus.BOOKED || slot.getBookingUser() == null) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.ONLY_BOOKED_APPOINTMENTS_CANCELLABLE);
        }
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setBookingUser(null);
        slot.setBookedAt(null);
        slot.setDoneAt(null);
        return slot;
    }

    private void createSlots(EinAb einAb, int count) {
        List<Slot> slots = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Slot slot = new Slot();
            slot.setEinAb(einAb);
            slot.setStatus(SlotStatus.AVAILABLE);
            slots.add(slot);
        }
        slotRepository.saveAll(slots);
    }

    private void ensureTeacherActive(User teacher) {
        if (!teacher.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.TEACHER_INACTIVE);
        }
    }

    private void validateSlotCount(int slotCount) {
        if (slotCount < 1 || slotCount > 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.SLOT_COUNT_OUT_OF_RANGE);
        }
    }

    private void validateEinAb(EinAbCategory category, int slotCount, String publicLocation, String onlineCallLink, Integer minimumPickupCount) {
        if (isOnline(category)) {
            normalizeRequiredOnlineCallLink(onlineCallLink);
        } else {
            validateSlotCount(slotCount);
            normalizeRequiredLocation(publicLocation);
            normalizeMinimumPickupCount(minimumPickupCount);
        }
    }

    private boolean isOnline(EinAbCategory category) {
        return category == EinAbCategory.ONLINE;
    }

    private String normalizeLocation(String location) {
        return location == null || location.isBlank() ? null : location.trim();
    }

    private String normalizeRequiredLocation(String location) {
        String normalized = normalizeLocation(location);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.PUBLIC_LOCATION_REQUIRED);
        }
        return normalized;
    }

    private String normalizeRequiredOnlineCallLink(String onlineCallLink) {
        String normalized = normalizeLocation(onlineCallLink);
        if (normalized == null || normalized.length() > 2000
                || !(normalized.startsWith("https://") || normalized.startsWith("http://"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("onlineCallLink"));
        }
        return normalized;
    }

    private String normalizeWhatToBring(String whatToBring) {
        return whatToBring == null || whatToBring.isBlank() ? null : whatToBring.trim();
    }

    private Integer normalizeMinimumPickupCount(Integer minimumPickupCount) {
        if (minimumPickupCount == null) {
            return null;
        }
        if (minimumPickupCount < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, List.of("minimumPickupCount"));
        }
        return minimumPickupCount;
    }

    private EinAb requireTeacherEinAb(User teacher, UUID einAbId, boolean admin, Bezirk bezirk) {
        EinAb einAb = einAbRepository.findWithTeacherByIdAndBezirk(einAbId, bezirk)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.EINAB_NOT_FOUND));
        if (!admin && !einAb.getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ONLY_OWN_EINABS_MANAGEABLE);
        }
        return einAb;
    }

    private EinAb reloadEinAbWithTeacher(UUID einAbId, Bezirk bezirk) {
        return einAbRepository.findWithTeacherByIdAndBezirk(einAbId, bezirk)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.EINAB_NOT_FOUND));
    }

    private User requireBookingUser(UUID bookingUserId, Bezirk bezirk) {
        return userRepository.findWithBezirkById(bookingUserId)
                .filter(user -> user.getBezirk() != null && user.getBezirk().getId().equals(bezirk.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    private void ensureTeacherBezirk(User teacher, Bezirk bezirk) {
        if (teacher.getBezirk() == null || !teacher.getBezirk().getId().equals(bezirk.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_BEZIRK_MISMATCH);
        }
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

}
