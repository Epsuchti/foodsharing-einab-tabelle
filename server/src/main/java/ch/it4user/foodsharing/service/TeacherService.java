package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.EinAbRepository;
import ch.it4user.foodsharing.repository.BookingCommentRepository;
import ch.it4user.foodsharing.repository.UserRepository;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final FoodsharingClient foodsharingClient;

    public TeacherService(UserRepository userRepository,
                          EinAbRepository einAbRepository,
                          SlotRepository slotRepository,
                          BookingCommentRepository bookingCommentRepository,
                          IcalImportService icalImportService,
                          FoodsharingClient foodsharingClient) {
        this.userRepository = userRepository;
        this.einAbRepository = einAbRepository;
        this.slotRepository = slotRepository;
        this.bookingCommentRepository = bookingCommentRepository;
        this.icalImportService = icalImportService;
        this.foodsharingClient = foodsharingClient;
    }

    @Transactional
    public User signup(String foodsharingId, String icalLink, LanguageCode language) {
        String normalizedFoodsharingId = foodsharingId.trim();
        FoodsharingUserInfo foodsharingUser = foodsharingClient.getUser(normalizedFoodsharingId);
        User teacher = userRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId).orElseGet(User::new);
        teacher.setFoodsharingId(foodsharingUser.foodsharingId());
        teacher.setName(foodsharingUser.name());
        teacher.setTeacher(true);
        teacher.setActive(true);
        teacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        teacher.setPreferredLanguage(language);
        return userRepository.save(teacher);
    }

    @Transactional
    public User updateProfile(User teacher, String phoneNumber, String icalLink, LanguageCode language) {
        User managedTeacher = userRepository.findById(teacher.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        managedTeacher.setPhoneNumber(phoneNumber == null ? null : phoneNumber.trim());
        managedTeacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        managedTeacher.setPreferredLanguage(language);
        return managedTeacher;
    }

    public Page<EinAb> findTeacherEinAbs(User teacher, int page, int size) {
        return einAbRepository.findAllByTeacherOrderByStartDateTimeAsc(
                teacher,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public Page<Slot> findTeacherBookings(User teacher, int page, int size) {
        return slotRepository.findAllByTeacherAndStatuses(
                teacher,
                Set.of(SlotStatus.PENDING_CONFIRMATION, SlotStatus.BOOKED, SlotStatus.DONE),
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public List<BookingComment> findBookingComments(UUID bookingUserId) {
        User bookingUser = requireBookingUser(bookingUserId);
        if (!bookingUser.isActive()) {
            return List.of();
        }
        return bookingCommentRepository.findAllByBookingUserOrderByCreatedAtAsc(bookingUser);
    }

    @Transactional
    public BookingComment addBookingComment(User teacher, UUID bookingUserId, String comment) {
        User bookingUser = requireBookingUser(bookingUserId);
        if (!bookingUser.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.BOOKING_USER_DISABLED);
        }
        BookingComment bookingComment = new BookingComment();
        bookingComment.setBookingUser(bookingUser);
        bookingComment.setTeacher(teacher);
        bookingComment.setComment(comment.trim());
        return bookingCommentRepository.save(bookingComment);
    }

    public List<ch.it4user.foodsharing.openapi.model.IcalCandidate> getIcalCandidates(User teacher) {
        return icalImportService.loadCandidates(teacher.getIcalLink());
    }

    @Transactional
    public EinAb createEinAb(User teacher,
                             EinAbCategory category,
                             Instant startDateTime,
                             String location,
                             String publicLocation,
                             String whatToBring,
                             String hint,
                             boolean visitFairteiler,
                             int slotCount,
                             Integer minimumPickupCount) {
        ensureTeacherActive(teacher);
        validateEinAb(slotCount, publicLocation, minimumPickupCount);
        EinAb einAb = new EinAb();
        einAb.setTeacher(teacher);
        einAb.setCategory(category);
        einAb.setStartDateTime(startDateTime);
        einAb.setLocation(normalizeLocation(location));
        einAb.setPublicLocation(normalizeRequiredLocation(publicLocation));
        einAb.setWhatToBring(normalizeWhatToBring(whatToBring));
        einAb.setHint(normalizeWhatToBring(hint));
        einAb.setVisitFairteiler(visitFairteiler);
        einAb.setSlotCount(slotCount);
        einAb.setMinimumPickupCount(normalizeMinimumPickupCount(minimumPickupCount));
        EinAb savedEinAb = einAbRepository.save(einAb);
        createSlots(savedEinAb, slotCount);
        return reloadEinAbWithTeacher(savedEinAb.getId());
    }

    @Transactional
    public EinAb updateEinAb(User teacher,
                             UUID einAbId,
                             EinAbCategory category,
                             Instant startDateTime,
                             String location,
                             String publicLocation,
                             String whatToBring,
                             String hint,
                             boolean visitFairteiler,
                             int slotCount,
                             Integer minimumPickupCount,
                             boolean admin) {
        validateEinAb(slotCount, publicLocation, minimumPickupCount);
        EinAb einAb = requireTeacherEinAb(teacher, einAbId, admin);
        if (!admin) {
            ensureTeacherActive(teacher);
        }
        int existingSlots = slotRepository.findAllByEinAbOrderByCreatedAtAsc(einAb).size();
        if (slotCount < existingSlots) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.SLOT_COUNT_REDUCTION_NOT_SUPPORTED);
        }

        einAb.setCategory(category);
        einAb.setStartDateTime(startDateTime);
        einAb.setLocation(normalizeLocation(location));
        einAb.setPublicLocation(normalizeRequiredLocation(publicLocation));
        einAb.setWhatToBring(normalizeWhatToBring(whatToBring));
        einAb.setHint(normalizeWhatToBring(hint));
        einAb.setVisitFairteiler(visitFairteiler);
        einAb.setSlotCount(slotCount);
        einAb.setMinimumPickupCount(normalizeMinimumPickupCount(minimumPickupCount));
        if (slotCount > existingSlots) {
            createSlots(einAb, slotCount - existingSlots);
        }
        return reloadEinAbWithTeacher(einAb.getId());
    }

    @Transactional
    public void deleteEinAb(User teacher, UUID einAbId, boolean admin) {
        EinAb einAb = requireTeacherEinAb(teacher, einAbId, admin);
        if (slotRepository.existsByEinAbAndStatusIn(einAb, BLOCKING_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.BOOKED_SLOTS_PREVENT_DELETE);
        }
        List<Slot> slots = slotRepository.findAllByEinAbOrderByCreatedAtAsc(einAb);
        slotRepository.deleteAll(slots);
        einAbRepository.delete(einAb);
    }

    public Page<EinAb> findAllEinAbs(int page, int size) {
        return einAbRepository.findAllByOrderByStartDateTimeAsc(PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public Page<User> findAllTeachers(int page, int size) {
        return userRepository.findAllByTeacherTrueOrderByNameAsc(PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    @Transactional
    public User setTeacherActive(UUID teacherId, boolean active) {
        User teacher = userRepository.findById(teacherId)
                .filter(User::isTeacher)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        teacher.setActive(active);
        return teacher;
    }

    @Transactional
    public User setTeacherAdmin(UUID teacherId, boolean admin) {
        User teacher = userRepository.findById(teacherId)
                .filter(User::isTeacher)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        teacher.setAdmin(admin);
        if (admin) {
            teacher.setActive(true);
        }
        return teacher;
    }

    @Transactional
    public Slot cancelBookedSlot(User teacher, UUID slotId, boolean admin) {
        Slot slot = slotRepository.findForUpdateById(slotId)
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

    private void validateEinAb(int slotCount, String publicLocation, Integer minimumPickupCount) {
        validateSlotCount(slotCount);
        normalizeRequiredLocation(publicLocation);
        normalizeMinimumPickupCount(minimumPickupCount);
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

    private EinAb requireTeacherEinAb(User teacher, UUID einAbId, boolean admin) {
        EinAb einAb = einAbRepository.findById(einAbId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.EINAB_NOT_FOUND));
        if (!admin && !einAb.getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ONLY_OWN_EINABS_MANAGEABLE);
        }
        return einAb;
    }

    private EinAb reloadEinAbWithTeacher(UUID einAbId) {
        return einAbRepository.findWithTeacherById(einAbId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.EINAB_NOT_FOUND));
    }

    private User requireBookingUser(UUID bookingUserId) {
        return userRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

}
