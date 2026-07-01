package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.EinAbRepository;
import ch.it4user.foodsharing.repository.BookingCommentRepository;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
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

    private static final Set<SlotStatus> BLOCKING_STATUSES = Set.of(SlotStatus.BOOKED, SlotStatus.DONE);
    private static final String IMMUTABLE_ADMIN_EMAIL = "ez@it4user.ch";

    private final TeacherRepository teacherRepository;
    private final EinAbRepository einAbRepository;
    private final SlotRepository slotRepository;
    private final BookingUserRepository bookingUserRepository;
    private final BookingCommentRepository bookingCommentRepository;
    private final NotificationService notificationService;
    private final IcalImportService icalImportService;

    public TeacherService(TeacherRepository teacherRepository,
                          EinAbRepository einAbRepository,
                          SlotRepository slotRepository,
                          BookingUserRepository bookingUserRepository,
                          BookingCommentRepository bookingCommentRepository,
                          NotificationService notificationService,
                          IcalImportService icalImportService) {
        this.teacherRepository = teacherRepository;
        this.einAbRepository = einAbRepository;
        this.slotRepository = slotRepository;
        this.bookingUserRepository = bookingUserRepository;
        this.bookingCommentRepository = bookingCommentRepository;
        this.notificationService = notificationService;
        this.icalImportService = icalImportService;
    }

    @Transactional
    public Teacher signup(String email, String foodsharingId, String name, String phoneNumber, String icalLink, LanguageCode language) {
        if (teacherRepository.existsByEmailIgnoreCase(email.trim().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.TEACHER_EMAIL_ALREADY_EXISTS);
        }
        if (teacherRepository.existsByFoodsharingIdIgnoreCase(foodsharingId.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.TEACHER_FOODSHARING_ID_ALREADY_EXISTS);
        }
        Teacher teacher = new Teacher();
        teacher.setEmail(email.trim().toLowerCase());
        teacher.setFoodsharingId(foodsharingId.trim());
        teacher.setName(name.trim());
        teacher.setPhoneNumber(phoneNumber.trim());
        teacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        teacher.setPreferredLanguage(language);
        boolean immutableAdmin = IMMUTABLE_ADMIN_EMAIL.equalsIgnoreCase(teacher.getEmail());
        teacher.setAdmin(immutableAdmin);
        teacher.setActive(immutableAdmin);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher updateProfile(Teacher teacher, String phoneNumber, String icalLink, LanguageCode language) {
        teacher.setPhoneNumber(phoneNumber.trim());
        teacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        teacher.setPreferredLanguage(language);
        return teacher;
    }

    public Page<EinAb> findTeacherEinAbs(Teacher teacher, int page, int size) {
        return einAbRepository.findAllByTeacherOrderByStartDateTimeAsc(
                teacher,
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public Page<Slot> findTeacherBookings(Teacher teacher, int page, int size) {
        return slotRepository.findAllByTeacherAndStatuses(
                teacher,
                Set.of(SlotStatus.BOOKED, SlotStatus.DONE),
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    public List<BookingComment> findBookingComments(UUID bookingUserId) {
        BookingUser bookingUser = requireBookingUser(bookingUserId);
        if (!bookingUser.isActive()) {
            return List.of();
        }
        return bookingCommentRepository.findAllByBookingUserOrderByCreatedAtAsc(bookingUser);
    }

    @Transactional
    public BookingComment addBookingComment(Teacher teacher, UUID bookingUserId, String comment) {
        BookingUser bookingUser = requireBookingUser(bookingUserId);
        if (!bookingUser.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.BOOKING_USER_DISABLED);
        }
        BookingComment bookingComment = new BookingComment();
        bookingComment.setBookingUser(bookingUser);
        bookingComment.setTeacher(teacher);
        bookingComment.setComment(comment.trim());
        return bookingCommentRepository.save(bookingComment);
    }

    public List<ch.it4user.foodsharing.openapi.model.IcalCandidate> getIcalCandidates(Teacher teacher) {
        return icalImportService.loadCandidates(teacher.getIcalLink());
    }

    @Transactional
    public EinAb createEinAb(Teacher teacher,
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
        notificationService.notifyNewEinAb(savedEinAb);
        return savedEinAb;
    }

    @Transactional
    public EinAb updateEinAb(Teacher teacher,
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
        return einAb;
    }

    @Transactional
    public void deleteEinAb(Teacher teacher, UUID einAbId, boolean admin) {
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

    public Page<Teacher> findAllTeachers(int page, int size) {
        return teacherRepository.findAllByOrderByNameAsc(PageRequest.of(Math.max(page, 0), normalizeSize(size)));
    }

    @Transactional
    public Teacher setTeacherActive(UUID teacherId, boolean active) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        teacher.setActive(active);
        return teacher;
    }

    @Transactional
    public Teacher setTeacherAdmin(UUID teacherId, boolean admin) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.TEACHER_NOT_FOUND));
        if (IMMUTABLE_ADMIN_EMAIL.equalsIgnoreCase(teacher.getEmail()) && !admin) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.DEFAULT_ADMIN_CANNOT_BE_CHANGED);
        }
        teacher.setAdmin(admin);
        if (admin) {
            teacher.setActive(true);
        }
        return teacher;
    }

    @Transactional
    public Slot cancelBookedSlot(Teacher teacher, UUID slotId, boolean admin) {
        Slot slot = slotRepository.findForUpdateById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.SLOT_NOT_FOUND));
        if (!admin && !slot.getEinAb().getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ONLY_OWN_EINABS_MANAGEABLE);
        }
        if (slot.getStatus() != SlotStatus.BOOKED || slot.getBookingUser() == null) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.ONLY_BOOKED_APPOINTMENTS_CANCELLABLE);
        }
        notificationService.notifyTeacherCancelledBooking(slot);
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

    private void ensureTeacherActive(Teacher teacher) {
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

    private EinAb requireTeacherEinAb(Teacher teacher, UUID einAbId, boolean admin) {
        EinAb einAb = einAbRepository.findById(einAbId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.EINAB_NOT_FOUND));
        if (!admin && !einAb.getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ONLY_OWN_EINABS_MANAGEABLE);
        }
        return einAb;
    }

    private BookingUser requireBookingUser(UUID bookingUserId) {
        return bookingUserRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

}
