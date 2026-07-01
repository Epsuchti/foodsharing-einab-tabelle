package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.openapi.api.TeacherApi;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingCommentListResponse;
import ch.it4user.foodsharing.openapi.model.BookingCommentResponse;
import ch.it4user.foodsharing.openapi.model.CreateBookingCommentRequest;
import ch.it4user.foodsharing.openapi.model.IcalCandidateListResponse;
import ch.it4user.foodsharing.openapi.model.SlotResponse;
import ch.it4user.foodsharing.openapi.model.UpdateTeacherMeRequest;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbResponse;
import ch.it4user.foodsharing.openapi.model.TeacherSelfResponse;
import ch.it4user.foodsharing.openapi.model.UpsertEinAbRequest;
import ch.it4user.foodsharing.openapi.model.Language;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.service.CurrentActorService;
import ch.it4user.foodsharing.service.TeacherService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeacherController implements TeacherApi {

    private final TeacherService teacherService;
    private final CurrentActorService currentActorService;
    private final SlotRepository slotRepository;
    private final ApiModelMapper mapper;

    public TeacherController(TeacherService teacherService,
                             CurrentActorService currentActorService,
                             SlotRepository slotRepository,
                             ApiModelMapper mapper) {
        this.teacherService = teacherService;
        this.currentActorService = currentActorService;
        this.slotRepository = slotRepository;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<TeacherSelfResponse> getTeacherMe() {
        Teacher teacher = currentActorService.requireTeacher();
        return ResponseEntity.ok(mapper.toTeacherSelfResponse(teacher, teacherService.getIcalCandidates(teacher)));
    }

    @Override
    public ResponseEntity<TeacherSelfResponse> updateTeacherMe(UpdateTeacherMeRequest updateTeacherMeRequest) {
        Teacher teacher = currentActorService.requireTeacher();
        Teacher updated = teacherService.updateProfile(
                teacher,
                updateTeacherMeRequest.getPhoneNumber(),
                updateTeacherMeRequest.getIcalLink(),
                mapLanguage(updateTeacherMeRequest.getLanguage()));
        return ResponseEntity.ok(mapper.toTeacherSelfResponse(updated, teacherService.getIcalCandidates(updated)));
    }

    @Override
    public ResponseEntity<TeacherEinAbListResponse> getTeacherEinAbs(Integer page, Integer size) {
        Teacher teacher = currentActorService.requireTeacher();
        org.springframework.data.domain.Page<EinAb> einAbs = teacherService.findTeacherEinAbs(teacher, page == null ? 0 : page, size == null ? 20 : size);
        return ResponseEntity.ok(mapTeacherEinAbs(einAbs));
    }

    @Override
    public ResponseEntity<TeacherEinAbResponse> createTeacherEinAb(UpsertEinAbRequest upsertEinAbRequest) {
        Teacher teacher = currentActorService.requireTeacher();
        EinAb einAb = teacherService.createEinAb(
                teacher,
                ch.it4user.foodsharing.domain.enumtype.EinAbCategory.valueOf(upsertEinAbRequest.getCategory().getValue()),
                toInstant(upsertEinAbRequest.getStartDateTime()),
                upsertEinAbRequest.getLocation(),
                upsertEinAbRequest.getPublicLocation(),
                upsertEinAbRequest.getWhatToBring(),
                upsertEinAbRequest.getHint(),
                upsertEinAbRequest.getVisitFairteiler(),
                upsertEinAbRequest.getSlotCount(),
                upsertEinAbRequest.getMinimumPickupCount()
        );
        return ResponseEntity.status(201).body(mapTeacherEinAb(einAb));
    }

    @Override
    public ResponseEntity<TeacherEinAbResponse> updateTeacherEinAb(UUID einAbId, UpsertEinAbRequest upsertEinAbRequest) {
        Teacher teacher = currentActorService.requireTeacher();
        EinAb einAb = teacherService.updateEinAb(
                teacher,
                einAbId,
                ch.it4user.foodsharing.domain.enumtype.EinAbCategory.valueOf(upsertEinAbRequest.getCategory().getValue()),
                toInstant(upsertEinAbRequest.getStartDateTime()),
                upsertEinAbRequest.getLocation(),
                upsertEinAbRequest.getPublicLocation(),
                upsertEinAbRequest.getWhatToBring(),
                upsertEinAbRequest.getHint(),
                upsertEinAbRequest.getVisitFairteiler(),
                upsertEinAbRequest.getSlotCount(),
                upsertEinAbRequest.getMinimumPickupCount(),
                currentActorService.isAdmin()
        );
        return ResponseEntity.ok(mapTeacherEinAb(einAb));
    }

    @Override
    public ResponseEntity<Void> deleteTeacherEinAb(UUID einAbId) {
        teacherService.deleteEinAb(currentActorService.requireTeacher(), einAbId, currentActorService.isAdmin());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BookingListResponse> getTeacherBookings(Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toBookingListResponse(
                teacherService.findTeacherBookings(currentActorService.requireTeacher(), page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<BookingCommentResponse> addTeacherBookingComment(
            UUID bookingUserId,
            CreateBookingCommentRequest createBookingCommentRequest) {
        return ResponseEntity.status(201).body(mapper.toBookingCommentResponse(
                teacherService.addBookingComment(
                        currentActorService.requireTeacher(),
                        bookingUserId,
                        createBookingCommentRequest.getComment())));
    }

    @Override
    public ResponseEntity<BookingCommentListResponse> getTeacherBookingComments(UUID bookingUserId) {
        return ResponseEntity.ok(mapper.toBookingCommentListResponse(
                teacherService.findBookingComments(bookingUserId)));
    }

    @Override
    public ResponseEntity<IcalCandidateListResponse> getTeacherIcalCandidates(
            Integer page,
            Integer size) {
        List<ch.it4user.foodsharing.openapi.model.IcalCandidate> candidates = teacherService.getIcalCandidates(currentActorService.requireTeacher());
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        int fromIndex = Math.min(safePage * safeSize, candidates.size());
        int toIndex = Math.min(fromIndex + safeSize, candidates.size());
        org.springframework.data.domain.Page<ch.it4user.foodsharing.openapi.model.IcalCandidate> candidatePage =
                new org.springframework.data.domain.PageImpl<>(
                        candidates.subList(fromIndex, toIndex),
                        org.springframework.data.domain.PageRequest.of(safePage, safeSize),
                        candidates.size());
        return ResponseEntity.ok(mapper.toIcalCandidateListResponse(candidatePage));
    }

    @Override
    public ResponseEntity<SlotResponse> cancelTeacherSlotBooking(UUID slotId) {
        return ResponseEntity.ok(mapper.toSlotResponse(
                teacherService.cancelBookedSlot(currentActorService.requireTeacher(), slotId, currentActorService.isAdmin())));
    }

    private TeacherEinAbListResponse mapTeacherEinAbs(org.springframework.data.domain.Page<EinAb> einAbs) {
        Map<UUID, List<ch.it4user.foodsharing.domain.entity.Slot>> slotsByEinAb = slotRepository
                .findAllByEinAbInOrderByEinAbStartDateTimeAsc(einAbs.getContent())
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getEinAb().getId()));
        return mapper.toTeacherEinAbListResponse(new org.springframework.data.domain.PageImpl<>(
                einAbs.getContent().stream()
                .map(einAb -> mapper.toTeacherEinAbResponse(einAb, slotsByEinAb.getOrDefault(einAb.getId(), List.of())))
                .toList(),
                einAbs.getPageable(),
                einAbs.getTotalElements()));
    }

    private TeacherEinAbResponse mapTeacherEinAb(EinAb einAb) {
        Map<UUID, List<ch.it4user.foodsharing.domain.entity.Slot>> slotsByEinAb = slotRepository
                .findAllByEinAbInOrderByEinAbStartDateTimeAsc(List.of(einAb))
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getEinAb().getId()));
        return mapper.toTeacherEinAbResponse(einAb, slotsByEinAb.getOrDefault(einAb.getId(), List.of()));
    }

    private ch.it4user.foodsharing.domain.enumtype.LanguageCode mapLanguage(Language language) {
        return ch.it4user.foodsharing.domain.enumtype.LanguageCode.fromCode(language == null ? "de" : language.getValue());
    }

    private java.time.Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
