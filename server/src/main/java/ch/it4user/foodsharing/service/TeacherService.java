package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.SlotComment;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.EinAbRepository;
import ch.it4user.foodsharing.repository.SlotCommentRepository;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private static final Set<SlotStatus> BLOCKING_STATUSES = Set.of(SlotStatus.BOOKED, SlotStatus.DONE);

    private final TeacherRepository teacherRepository;
    private final EinAbRepository einAbRepository;
    private final SlotRepository slotRepository;
    private final SlotCommentRepository slotCommentRepository;
    private final NotificationService notificationService;
    private final IcalImportService icalImportService;

    public TeacherService(TeacherRepository teacherRepository,
                          EinAbRepository einAbRepository,
                          SlotRepository slotRepository,
                          SlotCommentRepository slotCommentRepository,
                          NotificationService notificationService,
                          IcalImportService icalImportService) {
        this.teacherRepository = teacherRepository;
        this.einAbRepository = einAbRepository;
        this.slotRepository = slotRepository;
        this.slotCommentRepository = slotCommentRepository;
        this.notificationService = notificationService;
        this.icalImportService = icalImportService;
    }

    @Transactional
    public Teacher signup(String email, String foodsharingId, String name, String icalLink) {
        if (teacherRepository.existsByEmailIgnoreCase(email.trim().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "Teacher email already exists");
        }
        if (teacherRepository.existsByFoodsharingIdIgnoreCase(foodsharingId.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Teacher foodsharing ID already exists");
        }
        Teacher teacher = new Teacher();
        teacher.setEmail(email.trim().toLowerCase());
        teacher.setFoodsharingId(foodsharingId.trim());
        teacher.setName(name.trim());
        teacher.setIcalLink(icalLink == null || icalLink.isBlank() ? null : icalLink.trim());
        teacher.setActive(false);
        return teacherRepository.save(teacher);
    }

    public List<EinAb> findTeacherEinAbs(Teacher teacher) {
        return einAbRepository.findAllByTeacherOrderByStartDateTimeAsc(teacher);
    }

    public List<Slot> findTeacherBookings(Teacher teacher) {
        return slotRepository.findAllByTeacherAndStatuses(teacher, Set.of(SlotStatus.BOOKED, SlotStatus.DONE));
    }

    public List<SlotComment> findSlotComments(Teacher teacher, UUID slotId, boolean admin) {
        Slot slot = requireTeacherSlot(teacher, slotId, admin);
        return slotCommentRepository.findAllBySlotOrderByCreatedAtAsc(slot);
    }

    @Transactional
    public SlotComment addSlotComment(Teacher teacher, UUID slotId, String comment, boolean admin) {
        Slot slot = requireTeacherSlot(teacher, slotId, admin);
        SlotComment slotComment = new SlotComment();
        slotComment.setSlot(slot);
        slotComment.setTeacher(teacher);
        slotComment.setComment(comment.trim());
        return slotCommentRepository.save(slotComment);
    }

    @Transactional
    public Slot updateSlotStatus(Teacher teacher, UUID slotId, SlotStatus status, boolean admin) {
        Slot slot = requireTeacherSlot(teacher, slotId, admin);
        if (status == SlotStatus.AVAILABLE) {
            slot.setBookingUser(null);
            slot.setBookedAt(null);
            slot.setDoneAt(null);
        }
        if (status == SlotStatus.DONE) {
            slot.setDoneAt(OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            slot.setDoneAt(null);
        }
        slot.setStatus(status);
        return slot;
    }

    public List<ch.it4user.foodsharing.openapi.model.IcalCandidate> getIcalCandidates(Teacher teacher) {
        return icalImportService.loadCandidates(teacher.getIcalLink());
    }

    @Transactional
    public EinAb createEinAb(Teacher teacher,
                             EinAbCategory category,
                             OffsetDateTime startDateTime,
                             String location,
                             boolean visitFairteiler,
                             int slotCount) {
        ensureTeacherActive(teacher);
        validateSlotCount(slotCount);
        EinAb einAb = new EinAb();
        einAb.setTeacher(teacher);
        einAb.setCategory(category);
        einAb.setStartDateTime(startDateTime);
        einAb.setLocation(normalizeLocation(location));
        einAb.setVisitFairteiler(visitFairteiler);
        einAb.setSlotCount(slotCount);
        EinAb savedEinAb = einAbRepository.save(einAb);
        createSlots(savedEinAb, slotCount);
        notificationService.notifyNewEinAb(savedEinAb);
        return savedEinAb;
    }

    @Transactional
    public EinAb updateEinAb(Teacher teacher,
                             UUID einAbId,
                             EinAbCategory category,
                             OffsetDateTime startDateTime,
                             String location,
                             boolean visitFairteiler,
                             int slotCount,
                             boolean admin) {
        validateSlotCount(slotCount);
        EinAb einAb = requireTeacherEinAb(teacher, einAbId, admin);
        if (!admin) {
            ensureTeacherActive(teacher);
        }
        int existingSlots = slotRepository.findAllByEinAbOrderByCreatedAtAsc(einAb).size();
        if (slotCount < existingSlots) {
            throw new ApiException(HttpStatus.CONFLICT, "Reducing slot count is not supported once slots exist");
        }

        einAb.setCategory(category);
        einAb.setStartDateTime(startDateTime);
        einAb.setLocation(normalizeLocation(location));
        einAb.setVisitFairteiler(visitFairteiler);
        einAb.setSlotCount(slotCount);
        if (slotCount > existingSlots) {
            createSlots(einAb, slotCount - existingSlots);
        }
        return einAb;
    }

    @Transactional
    public void deleteEinAb(Teacher teacher, UUID einAbId, boolean admin) {
        EinAb einAb = requireTeacherEinAb(teacher, einAbId, admin);
        if (slotRepository.existsByEinAbAndStatusIn(einAb, BLOCKING_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, "Booked or completed slots prevent deleting this EinAb");
        }
        List<Slot> slots = slotRepository.findAllByEinAbOrderByCreatedAtAsc(einAb);
        slotRepository.deleteAll(slots);
        einAbRepository.delete(einAb);
    }

    public List<EinAb> findAllEinAbs() {
        return einAbRepository.findAllByOrderByStartDateTimeAsc();
    }

    public List<Teacher> findAllTeachers() {
        return teacherRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Teacher::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public Teacher setTeacherActive(UUID teacherId, boolean active) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Teacher not found"));
        teacher.setActive(active);
        return teacher;
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
            throw new ApiException(HttpStatus.FORBIDDEN, "Teacher must be active to manage EinAbs");
        }
    }

    private void validateSlotCount(int slotCount) {
        if (slotCount < 1 || slotCount > 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slot count must be between 1 and 3");
        }
    }

    private String normalizeLocation(String location) {
        return location == null || location.isBlank() ? null : location.trim();
    }

    private EinAb requireTeacherEinAb(Teacher teacher, UUID einAbId, boolean admin) {
        EinAb einAb = einAbRepository.findById(einAbId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EinAb not found"));
        if (!admin && !einAb.getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own EinAbs");
        }
        return einAb;
    }

    private Slot requireTeacherSlot(Teacher teacher, UUID slotId, boolean admin) {
        Slot slot = slotRepository.findDetailedById(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!admin && !slot.getEinAb().getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own slots");
        }
        return slot;
    }
}
