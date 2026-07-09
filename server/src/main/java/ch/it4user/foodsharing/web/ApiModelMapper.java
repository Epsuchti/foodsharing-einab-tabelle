package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserResponse;
import ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.AvailableSlotListResponse;
import ch.it4user.foodsharing.openapi.model.AvailableSlotResponse;
import ch.it4user.foodsharing.openapi.model.BookingCommentListResponse;
import ch.it4user.foodsharing.openapi.model.BookingCommentResponse;
import ch.it4user.foodsharing.openapi.model.BookingDetailResponse;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.EinAbResponse;
import ch.it4user.foodsharing.openapi.model.IcalCandidateListResponse;
import ch.it4user.foodsharing.openapi.model.SlotResponse;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbResponse;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.openapi.model.TeacherSelfResponse;
import ch.it4user.foodsharing.openapi.model.NotificationSubscriptionResponse;
import ch.it4user.foodsharing.service.AdminService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ApiModelMapper {

    public TeacherResponse toTeacherResponse(User teacher) {
        TeacherResponse response = new TeacherResponse();
        response.setId(teacher.getId());
        response.setEmail(teacher.getEmail());
        response.setFoodsharingId(teacher.getFoodsharingId());
        response.setName(teacher.getName());
        response.setPhoneNumber(teacher.getPhoneNumber());
        response.setIcalLink(teacher.getIcalLink());
        response.setActive(teacher.isActive());
        response.setWantsToBeTeacher(teacher.isWantsToBeTeacher());
        response.setCanGiveEinAbs(teacher.isCanGiveEinAbs());
        response.setCanManageUsers(teacher.isCanManageUsers());
        response.setCanUseAutomations(teacher.isCanUseAutomations());
        response.setCanSeeUserPickupCountGrouping(teacher.isCanSeeUserPickupCountGrouping());
        response.setCanUseAutomationSlotApproval(teacher.isCanUseAutomationSlotApproval());
        response.setCanSeeAllAutomationDecisions(teacher.isCanSeeAllAutomationDecisions());
        response.setLanguage(ch.it4user.foodsharing.openapi.model.Language.fromValue(teacher.getPreferredLanguage().getCode()));
        response.setCreatedAt(toOffsetDateTime(teacher.getCreatedAt()));
        response.setUpdatedAt(toOffsetDateTime(teacher.getUpdatedAt()));
        return response;
    }

    public NotificationSubscriptionResponse toNotificationSubscriptionResponse(NotificationSubscription subscription) {
        NotificationSubscriptionResponse response = new NotificationSubscriptionResponse();
        response.setEmail(subscription.getEmail());
        response.setActive(subscription.isActive());
        return response;
    }

    public TeacherSelfResponse toTeacherSelfResponse(User teacher, List<ch.it4user.foodsharing.openapi.model.IcalCandidate> candidates) {
        TeacherSelfResponse response = new TeacherSelfResponse();
        copyTeacher(toTeacherResponse(teacher), response);
        response.setIcalCandidates(candidates);
        return response;
    }

    public TeacherListResponse toTeacherListResponse(Page<User> teachers) {
        TeacherListResponse response = new TeacherListResponse();
        response.setTeachers(teachers.getContent().stream().map(this::toTeacherResponse).toList());
        fillPage(response, teachers);
        return response;
    }

    public BookingUserResponse toBookingUserResponse(User bookingUser) {
        BookingUserResponse response = new BookingUserResponse();
        response.setId(bookingUser.getId());
        response.setEmail(bookingUser.getEmail());
        response.setName(bookingUser.getName());
        response.setFoodsharingId(bookingUser.getFoodsharingId());
        response.setPhoneNumber(bookingUser.getPhoneNumber());
        response.setLanguage(ch.it4user.foodsharing.openapi.model.Language.fromValue(bookingUser.getPreferredLanguage().getCode()));
        response.setCanGiveEinAbs(bookingUser.isCanGiveEinAbs());
        response.setCanManageUsers(bookingUser.isCanManageUsers());
        response.setCanUseAutomations(bookingUser.isCanUseAutomations());
        response.setCanSeeUserPickupCountGrouping(bookingUser.isCanSeeUserPickupCountGrouping());
        response.setCanUseAutomationSlotApproval(bookingUser.isCanUseAutomationSlotApproval());
        response.setCanSeeAllAutomationDecisions(bookingUser.isCanSeeAllAutomationDecisions());
        response.setCreatedAt(toOffsetDateTime(bookingUser.getCreatedAt()));
        response.setUpdatedAt(toOffsetDateTime(bookingUser.getUpdatedAt()));
        return response;
    }

    public AdminBookingUserPageResponse toAdminBookingUserPageResponse(AdminService.AdminUsersView view) {
        AdminBookingUserPageResponse response = new AdminBookingUserPageResponse();
        response.setUsers(view.users().getContent().stream()
                .map(user -> toAdminBookingUserResponse(user, view.bookingsByUser(), view.commentsByUser()))
                .toList());
        fillPage(response, view.users());
        return response;
    }

    public AdminBookingUserResponse toAdminBookingUserResponse(User bookingUser,
                                                               Map<UUID, List<Slot>> bookingsByUser,
                                                               Map<UUID, List<BookingComment>> commentsByUser) {
        AdminBookingUserResponse response = new AdminBookingUserResponse();
        response.setUser(toBookingUserResponse(bookingUser));
        List<Slot> bookings = bookingsByUser.getOrDefault(bookingUser.getId(), List.of());
        response.setBookings(bookings.stream().map(this::toBookingDetailResponse).toList());
        response.setComments(commentsByUser.getOrDefault(bookingUser.getId(), List.of()).stream().map(this::toBookingCommentResponse).toList());
        response.setPickupCount(bookings.size());
        return response;
    }

    public AvailableSlotListResponse toAvailableSlotListResponse(Page<Slot> slots) {
        AvailableSlotListResponse response = new AvailableSlotListResponse();
        response.setSlots(slots.getContent().stream().map(this::toAvailableSlotResponse).toList());
        fillPage(response, slots);
        return response;
    }

    public AvailableSlotResponse toAvailableSlotResponse(Slot slot) {
        AvailableSlotResponse response = new AvailableSlotResponse();
        response.setSlotId(slot.getId());
        response.setCategory(ch.it4user.foodsharing.openapi.model.EinAbCategory.fromValue(slot.getEinAb().getCategory().name()));
        response.setStartDateTime(toOffsetDateTime(slot.getEinAb().getStartDateTime()));
        response.setLocation(slot.getEinAb().getPublicLocation());
        response.setPublicLocation(slot.getEinAb().getPublicLocation());
        response.setTeacherName(slot.getEinAb().getTeacher().getName());
        response.setTeacherId(slot.getEinAb().getTeacher().getId());
        response.setMinimumPickupCount(slot.getEinAb().getMinimumPickupCount());
        response.setVisitFairteiler(slot.getEinAb().isVisitFairteiler());
        response.setStatus(ch.it4user.foodsharing.openapi.model.SlotStatus.fromValue(slot.getStatus().name()));
        return response;
    }

    public BookingDetailResponse toBookingDetailResponse(Slot slot) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setSlotId(slot.getId());
        response.setEinAbId(slot.getEinAb().getId());
        response.setCategory(ch.it4user.foodsharing.openapi.model.EinAbCategory.fromValue(slot.getEinAb().getCategory().name()));
        response.setStartDateTime(toOffsetDateTime(slot.getEinAb().getStartDateTime()));
        response.setLocation(slot.getEinAb().getLocation());
        response.setPublicLocation(slot.getEinAb().getPublicLocation());
        response.setWhatToBring(slot.getEinAb().getWhatToBring());
        response.setHint(slot.getEinAb().getHint());
        response.setTeacherName(slot.getEinAb().getTeacher().getName());
        response.setTeacherPhoneNumber(slot.getEinAb().getTeacher().getPhoneNumber());
        response.setVisitFairteiler(slot.getEinAb().isVisitFairteiler());
        response.setStatus(ch.it4user.foodsharing.openapi.model.SlotStatus.fromValue(slot.getStatus().name()));
        if (slot.getBookingUser() != null) {
            response.setBookingUser(toBookingUserResponse(slot.getBookingUser()));
        }
        return response;
    }

    public BookingListResponse toBookingListResponse(Page<Slot> slots) {
        BookingListResponse response = new BookingListResponse();
        response.setBookings(slots.getContent().stream().map(this::toBookingDetailResponse).toList());
        fillPage(response, slots);
        return response;
    }

    public SlotResponse toSlotResponse(Slot slot) {
        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());
        response.setEinAbId(slot.getEinAb().getId());
        response.setStatus(ch.it4user.foodsharing.openapi.model.SlotStatus.fromValue(slot.getStatus().name()));
        response.setBookedAt(toOffsetDateTime(slot.getBookedAt()));
        response.setDoneAt(toOffsetDateTime(slot.getDoneAt()));
        response.setCreatedAt(toOffsetDateTime(slot.getCreatedAt()));
        response.setUpdatedAt(toOffsetDateTime(slot.getUpdatedAt()));
        if (slot.getBookingUser() != null) {
            response.setBookingUser(toBookingUserResponse(slot.getBookingUser()));
        }
        return response;
    }

    public EinAbResponse toEinAbResponse(EinAb einAb) {
        EinAbResponse response = new EinAbResponse();
        response.setId(einAb.getId());
        response.setCategory(ch.it4user.foodsharing.openapi.model.EinAbCategory.fromValue(einAb.getCategory().name()));
        response.setStartDateTime(toOffsetDateTime(einAb.getStartDateTime()));
        response.setLocation(einAb.getLocation());
        response.setPublicLocation(einAb.getPublicLocation());
        response.setWhatToBring(einAb.getWhatToBring());
        response.setHint(einAb.getHint());
        response.setTeacher(toTeacherResponse(einAb.getTeacher()));
        response.setVisitFairteiler(einAb.isVisitFairteiler());
        response.setSlotCount(einAb.getSlotCount());
        response.setMinimumPickupCount(einAb.getMinimumPickupCount());
        response.setCreatedAt(toOffsetDateTime(einAb.getCreatedAt()));
        response.setUpdatedAt(toOffsetDateTime(einAb.getUpdatedAt()));
        return response;
    }

    public TeacherEinAbResponse toTeacherEinAbResponse(EinAb einAb, List<Slot> slots) {
        TeacherEinAbResponse response = new TeacherEinAbResponse();
        copyEinAb(toEinAbResponse(einAb), response);
        response.setSlots(slots.stream().map(this::toSlotResponse).toList());
        return response;
    }

    public TeacherEinAbListResponse toTeacherEinAbListResponse(Page<TeacherEinAbResponse> einAbs) {
        TeacherEinAbListResponse response = new TeacherEinAbListResponse();
        response.setEinAbs(einAbs.getContent());
        fillPage(response, einAbs);
        return response;
    }

    public AdminEinAbListResponse toAdminEinAbListResponse(Page<EinAb> einAbs) {
        AdminEinAbListResponse response = new AdminEinAbListResponse();
        response.setEinAbs(einAbs.getContent().stream().map(this::toEinAbResponse).toList());
        fillPage(response, einAbs);
        return response;
    }

    public BookingCommentResponse toBookingCommentResponse(BookingComment comment) {
        BookingCommentResponse response = new BookingCommentResponse();
        response.setId(comment.getId());
        response.setBookingUserId(comment.getBookingUser().getId());
        response.setTeacherId(comment.getTeacher().getId());
        response.setTeacherName(comment.getTeacher().getName());
        response.setComment(comment.getComment());
        response.setCreatedAt(toOffsetDateTime(comment.getCreatedAt()));
        return response;
    }

    public BookingCommentListResponse toBookingCommentListResponse(List<BookingComment> comments) {
        BookingCommentListResponse response = new BookingCommentListResponse();
        response.setComments(comments.stream().map(this::toBookingCommentResponse).toList());
        return response;
    }

    public IcalCandidateListResponse toIcalCandidateListResponse(Page<ch.it4user.foodsharing.openapi.model.IcalCandidate> candidates) {
        IcalCandidateListResponse response = new IcalCandidateListResponse();
        response.setCandidates(candidates.getContent());
        fillPage(response, candidates);
        return response;
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.TeacherListResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.AvailableSlotListResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.BookingListResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.TeacherEinAbListResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.IcalCandidateListResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void fillPage(ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse response, Page<?> page) {
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
    }

    private void copyTeacher(TeacherResponse source, TeacherSelfResponse target) {
        target.setId(source.getId());
        target.setEmail(source.getEmail());
        target.setFoodsharingId(source.getFoodsharingId());
        target.setName(source.getName());
        target.setPhoneNumber(source.getPhoneNumber());
        target.setIcalLink(source.getIcalLink());
        target.setActive(source.getActive());
        target.setWantsToBeTeacher(source.getWantsToBeTeacher());
        target.setCanGiveEinAbs(source.getCanGiveEinAbs());
        target.setCanManageUsers(source.getCanManageUsers());
        target.setCanUseAutomations(source.getCanUseAutomations());
        target.setCanSeeUserPickupCountGrouping(source.getCanSeeUserPickupCountGrouping());
        target.setCanUseAutomationSlotApproval(source.getCanUseAutomationSlotApproval());
        target.setCanSeeAllAutomationDecisions(source.getCanSeeAllAutomationDecisions());
        target.setLanguage(source.getLanguage());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private void copyEinAb(EinAbResponse source, TeacherEinAbResponse target) {
        target.setId(source.getId());
        target.setCategory(source.getCategory());
        target.setStartDateTime(source.getStartDateTime());
        target.setLocation(source.getLocation());
        target.setPublicLocation(source.getPublicLocation());
        target.setWhatToBring(source.getWhatToBring());
        target.setHint(source.getHint());
        target.setTeacher(source.getTeacher());
        target.setVisitFairteiler(source.getVisitFairteiler());
        target.setSlotCount(source.getSlotCount());
        target.setMinimumPickupCount(source.getMinimumPickupCount());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }
}
