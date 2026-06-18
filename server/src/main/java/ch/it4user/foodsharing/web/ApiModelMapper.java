package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.service.AdminService;
import ch.it4user.foodsharing.openapi.model.AdminEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserPageResponse;
import ch.it4user.foodsharing.openapi.model.AdminBookingUserResponse;
import ch.it4user.foodsharing.openapi.model.AvailableSlotListResponse;
import ch.it4user.foodsharing.openapi.model.AvailableSlotResponse;
import ch.it4user.foodsharing.openapi.model.BookingDetailResponse;
import ch.it4user.foodsharing.openapi.model.BookingListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserListResponse;
import ch.it4user.foodsharing.openapi.model.BookingUserResponse;
import ch.it4user.foodsharing.openapi.model.EinAbResponse;
import ch.it4user.foodsharing.openapi.model.NotificationSubscriptionResponse;
import ch.it4user.foodsharing.openapi.model.BookingCommentListResponse;
import ch.it4user.foodsharing.openapi.model.BookingCommentResponse;
import ch.it4user.foodsharing.openapi.model.SlotResponse;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherEinAbResponse;
import ch.it4user.foodsharing.openapi.model.TeacherListResponse;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.openapi.model.TeacherSelfResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ApiModelMapper {

    public TeacherResponse toTeacherResponse(Teacher teacher) {
        TeacherResponse response = new TeacherResponse();
        response.setId(teacher.getId());
        response.setEmail(teacher.getEmail());
        response.setFoodsharingId(teacher.getFoodsharingId());
        response.setName(teacher.getName());
        response.setIcalLink(teacher.getIcalLink());
        response.setActive(teacher.isActive());
        response.setCreatedAt(teacher.getCreatedAt());
        response.setUpdatedAt(teacher.getUpdatedAt());
        return response;
    }

    public TeacherSelfResponse toTeacherSelfResponse(Teacher teacher,
                                                     List<ch.it4user.foodsharing.openapi.model.IcalCandidate> candidates) {
        TeacherSelfResponse response = new TeacherSelfResponse();
        copyTeacher(toTeacherResponse(teacher), response);
        response.setIcalCandidates(candidates);
        return response;
    }

    public TeacherListResponse toTeacherListResponse(List<Teacher> teachers) {
        TeacherListResponse response = new TeacherListResponse();
        response.setTeachers(teachers.stream().map(this::toTeacherResponse).toList());
        return response;
    }

    public BookingUserResponse toBookingUserResponse(BookingUser bookingUser) {
        BookingUserResponse response = new BookingUserResponse();
        response.setId(bookingUser.getId());
        response.setEmail(bookingUser.getEmail());
        response.setName(bookingUser.getName());
        response.setFoodsharingId(bookingUser.getFoodsharingId());
        response.setPhoneNumber(bookingUser.getPhoneNumber());
        response.setCreatedAt(bookingUser.getCreatedAt());
        response.setUpdatedAt(bookingUser.getUpdatedAt());
        return response;
    }

    public BookingUserListResponse toBookingUserListResponse(List<BookingUser> users) {
        BookingUserListResponse response = new BookingUserListResponse();
        response.setUsers(users.stream().map(this::toBookingUserResponse).toList());
        return response;
    }

    public AdminBookingUserPageResponse toAdminBookingUserPageResponse(AdminService.AdminUsersView view) {
        AdminBookingUserPageResponse response = new AdminBookingUserPageResponse();
        response.setUsers(view.users().getContent().stream()
                .map(user -> toAdminBookingUserResponse(
                        user,
                        view.bookingsByUser(),
                        view.commentsByUser()))
                .toList());
        response.setPage(view.users().getNumber());
        response.setSize(view.users().getSize());
        response.setTotalElements(view.users().getTotalElements());
        response.setTotalPages(view.users().getTotalPages());
        response.setHasNext(view.users().hasNext());
        response.setHasPrevious(view.users().hasPrevious());
        return response;
    }

    public AdminBookingUserResponse toAdminBookingUserResponse(BookingUser bookingUser,
                                                               Map<UUID, List<Slot>> bookingsByUser,
                                                               Map<UUID, List<BookingComment>> commentsByUser) {
        AdminBookingUserResponse response = new AdminBookingUserResponse();
        response.setUser(toBookingUserResponse(bookingUser));
        List<Slot> bookings = bookingsByUser.getOrDefault(bookingUser.getId(), List.of());
        response.setBookings(bookings.stream().map(this::toBookingDetailResponse).toList());
        response.setComments(commentsByUser.getOrDefault(bookingUser.getId(), List.of())
                .stream()
                .map(this::toBookingCommentResponse)
                .toList());
        response.setPickupCount(bookings.size());
        return response;
    }

    public AvailableSlotListResponse toAvailableSlotListResponse(List<Slot> slots) {
        AvailableSlotListResponse response = new AvailableSlotListResponse();
        response.setSlots(slots.stream().map(this::toAvailableSlotResponse).toList());
        return response;
    }

    public AvailableSlotResponse toAvailableSlotResponse(Slot slot) {
        AvailableSlotResponse response = new AvailableSlotResponse();
        response.setSlotId(slot.getId());
        response.setCategory(ch.it4user.foodsharing.openapi.model.EinAbCategory.fromValue(slot.getEinAb().getCategory().name()));
        response.setStartDateTime(slot.getEinAb().getStartDateTime());
        response.setLocation(slot.getEinAb().getLocation());
        response.setTeacherName(slot.getEinAb().getTeacher().getName());
        response.setTeacherId(slot.getEinAb().getTeacher().getId());
        response.setVisitFairteiler(slot.getEinAb().isVisitFairteiler());
        response.setStatus(ch.it4user.foodsharing.openapi.model.SlotStatus.fromValue(slot.getStatus().name()));
        return response;
    }

    public BookingDetailResponse toBookingDetailResponse(Slot slot) {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setSlotId(slot.getId());
        response.setEinAbId(slot.getEinAb().getId());
        response.setCategory(ch.it4user.foodsharing.openapi.model.EinAbCategory.fromValue(slot.getEinAb().getCategory().name()));
        response.setStartDateTime(slot.getEinAb().getStartDateTime());
        response.setLocation(slot.getEinAb().getLocation());
        response.setTeacherName(slot.getEinAb().getTeacher().getName());
        response.setVisitFairteiler(slot.getEinAb().isVisitFairteiler());
        response.setStatus(ch.it4user.foodsharing.openapi.model.SlotStatus.fromValue(slot.getStatus().name()));
        if (slot.getBookingUser() != null) {
            response.setBookingUser(toBookingUserResponse(slot.getBookingUser()));
        }
        return response;
    }

    public BookingListResponse toBookingListResponse(List<Slot> slots) {
        BookingListResponse response = new BookingListResponse();
        response.setBookings(slots.stream().map(this::toBookingDetailResponse).toList());
        return response;
    }

    public SlotResponse toSlotResponse(Slot slot) {
        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());
        response.setEinAbId(slot.getEinAb().getId());
        response.setStatus(ch.it4user.foodsharing.openapi.model.SlotStatus.fromValue(slot.getStatus().name()));
        response.setBookedAt(slot.getBookedAt());
        response.setDoneAt(slot.getDoneAt());
        response.setCreatedAt(slot.getCreatedAt());
        response.setUpdatedAt(slot.getUpdatedAt());
        if (slot.getBookingUser() != null) {
            response.setBookingUser(toBookingUserResponse(slot.getBookingUser()));
        }
        return response;
    }

    public EinAbResponse toEinAbResponse(EinAb einAb) {
        EinAbResponse response = new EinAbResponse();
        response.setId(einAb.getId());
        response.setCategory(ch.it4user.foodsharing.openapi.model.EinAbCategory.fromValue(einAb.getCategory().name()));
        response.setStartDateTime(einAb.getStartDateTime());
        response.setLocation(einAb.getLocation());
        response.setTeacher(toTeacherResponse(einAb.getTeacher()));
        response.setVisitFairteiler(einAb.isVisitFairteiler());
        response.setSlotCount(einAb.getSlotCount());
        response.setCreatedAt(einAb.getCreatedAt());
        response.setUpdatedAt(einAb.getUpdatedAt());
        return response;
    }

    public TeacherEinAbResponse toTeacherEinAbResponse(EinAb einAb, List<Slot> slots) {
        TeacherEinAbResponse response = new TeacherEinAbResponse();
        copyEinAb(toEinAbResponse(einAb), response);
        response.setSlots(slots.stream().map(this::toSlotResponse).toList());
        return response;
    }

    public TeacherEinAbListResponse toTeacherEinAbListResponse(List<TeacherEinAbResponse> einAbs) {
        TeacherEinAbListResponse response = new TeacherEinAbListResponse();
        response.setEinAbs(einAbs);
        return response;
    }

    public AdminEinAbListResponse toAdminEinAbListResponse(List<EinAb> einAbs) {
        AdminEinAbListResponse response = new AdminEinAbListResponse();
        response.setEinAbs(einAbs.stream().map(this::toEinAbResponse).toList());
        return response;
    }

    public BookingCommentResponse toBookingCommentResponse(BookingComment comment) {
        BookingCommentResponse response = new BookingCommentResponse();
        response.setId(comment.getId());
        response.setBookingUserId(comment.getBookingUser().getId());
        response.setTeacherId(comment.getTeacher().getId());
        response.setTeacherName(comment.getTeacher().getName());
        response.setComment(comment.getComment());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }

    public BookingCommentListResponse toBookingCommentListResponse(List<BookingComment> comments) {
        BookingCommentListResponse response = new BookingCommentListResponse();
        response.setComments(comments.stream().map(this::toBookingCommentResponse).toList());
        return response;
    }

    public NotificationSubscriptionResponse toNotificationResponse(NotificationSubscription subscription) {
        NotificationSubscriptionResponse response = new NotificationSubscriptionResponse();
        response.setEmail(subscription.getEmail());
        response.setActive(subscription.isActive());
        return response;
    }

    private void copyTeacher(TeacherResponse source, TeacherSelfResponse target) {
        target.setId(source.getId());
        target.setEmail(source.getEmail());
        target.setFoodsharingId(source.getFoodsharingId());
        target.setName(source.getName());
        target.setIcalLink(source.getIcalLink());
        target.setActive(source.getActive());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private void copyEinAb(EinAbResponse source, TeacherEinAbResponse target) {
        target.setId(source.getId());
        target.setCategory(source.getCategory());
        target.setStartDateTime(source.getStartDateTime());
        target.setLocation(source.getLocation());
        target.setTeacher(source.getTeacher());
        target.setVisitFairteiler(source.getVisitFairteiler());
        target.setSlotCount(source.getSlotCount());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }
}
