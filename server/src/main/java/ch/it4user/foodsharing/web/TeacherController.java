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
import ch.it4user.foodsharing.openapi.model.TeacherEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbResponse;
import ch.it4user.foodsharing.openapi.model.TeacherSelfResponse;
import ch.it4user.foodsharing.openapi.model.UpdateSlotStatusRequest;
import ch.it4user.foodsharing.openapi.model.UpsertEinAbRequest;
import ch.it4user.foodsharing.repository.SlotRepository;
import ch.it4user.foodsharing.service.CurrentActorService;
import ch.it4user.foodsharing.service.TeacherService;
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
    public ResponseEntity<TeacherEinAbListResponse> getTeacherEinAbs() {
        Teacher teacher = currentActorService.requireTeacher();
        List<EinAb> einAbs = teacherService.findTeacherEinAbs(teacher);
        return ResponseEntity.ok(mapTeacherEinAbs(einAbs));
    }

    @Override
    public ResponseEntity<TeacherEinAbResponse> createTeacherEinAb(UpsertEinAbRequest upsertEinAbRequest) {
        Teacher teacher = currentActorService.requireTeacher();
        EinAb einAb = teacherService.createEinAb(
                teacher,
                ch.it4user.foodsharing.domain.enumtype.EinAbCategory.valueOf(upsertEinAbRequest.getCategory().getValue()),
                upsertEinAbRequest.getStartDateTime(),
                upsertEinAbRequest.getLocation(),
                upsertEinAbRequest.getVisitFairteiler(),
                upsertEinAbRequest.getSlotCount()
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
                upsertEinAbRequest.getStartDateTime(),
                upsertEinAbRequest.getLocation(),
                upsertEinAbRequest.getVisitFairteiler(),
                upsertEinAbRequest.getSlotCount(),
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
    public ResponseEntity<BookingListResponse> getTeacherBookings() {
        return ResponseEntity.ok(mapper.toBookingListResponse(
                teacherService.findTeacherBookings(currentActorService.requireTeacher())));
    }

    @Override
    public ResponseEntity<BookingCommentResponse> addTeacherBookingComment(UUID bookingUserId,
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
    public ResponseEntity<SlotResponse> updateTeacherSlotStatus(UUID slotId, UpdateSlotStatusRequest updateSlotStatusRequest) {
        return ResponseEntity.ok(mapper.toSlotResponse(
                teacherService.updateSlotStatus(
                        currentActorService.requireTeacher(),
                        slotId,
                        ch.it4user.foodsharing.domain.enumtype.SlotStatus.valueOf(updateSlotStatusRequest.getStatus().getValue()),
                        currentActorService.isAdmin())));
    }

    @Override
    public ResponseEntity<IcalCandidateListResponse> getTeacherIcalCandidates() {
        IcalCandidateListResponse response = new IcalCandidateListResponse();
        response.setCandidates(teacherService.getIcalCandidates(currentActorService.requireTeacher()));
        return ResponseEntity.ok(response);
    }

    private TeacherEinAbListResponse mapTeacherEinAbs(List<EinAb> einAbs) {
        Map<UUID, List<ch.it4user.foodsharing.domain.entity.Slot>> slotsByEinAb = slotRepository
                .findAllByEinAbInOrderByEinAbStartDateTimeAsc(einAbs)
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getEinAb().getId()));
        return mapper.toTeacherEinAbListResponse(einAbs.stream()
                .map(einAb -> mapper.toTeacherEinAbResponse(einAb, slotsByEinAb.getOrDefault(einAb.getId(), List.of())))
                .toList());
    }

    private TeacherEinAbResponse mapTeacherEinAb(EinAb einAb) {
        Map<UUID, List<ch.it4user.foodsharing.domain.entity.Slot>> slotsByEinAb = slotRepository
                .findAllByEinAbInOrderByEinAbStartDateTimeAsc(List.of(einAb))
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getEinAb().getId()));
        return mapper.toTeacherEinAbResponse(einAb, slotsByEinAb.getOrDefault(einAb.getId(), List.of()));
    }
}
