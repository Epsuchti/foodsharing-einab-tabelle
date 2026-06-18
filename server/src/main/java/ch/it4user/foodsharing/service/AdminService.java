package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.BookingCommentRepository;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final List<SlotStatus> ACTIVE_BOOKING_STATUSES = List.of(SlotStatus.BOOKED, SlotStatus.DONE);

    private final TeacherService teacherService;
    private final BookingUserService bookingUserService;
    private final SlotRepository slotRepository;
    private final BookingUserRepository bookingUserRepository;
    private final BookingCommentRepository bookingCommentRepository;

    public AdminService(TeacherService teacherService,
                        BookingUserService bookingUserService,
                        SlotRepository slotRepository,
                        BookingUserRepository bookingUserRepository,
                        BookingCommentRepository bookingCommentRepository) {
        this.teacherService = teacherService;
        this.bookingUserService = bookingUserService;
        this.slotRepository = slotRepository;
        this.bookingUserRepository = bookingUserRepository;
        this.bookingCommentRepository = bookingCommentRepository;
    }

    public List<Teacher> getTeachers() {
        return teacherService.findAllTeachers();
    }

    public Teacher setTeacherActive(java.util.UUID teacherId, boolean active) {
        return teacherService.setTeacherActive(teacherId, active);
    }

    public List<EinAb> getEinAbs() {
        return teacherService.findAllEinAbs();
    }

    public List<Slot> getBookings() {
        return slotRepository.findAllByStatusInOrderByEinAbStartDateTimeAsc(ACTIVE_BOOKING_STATUSES).stream()
                .filter(slot -> slot.getBookingUser() == null || slot.getBookingUser().isActive())
                .toList();
    }

    public AdminUsersView getUsers(int page, int size, boolean threePickupsOnly) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<BookingUser> bookingUsers = threePickupsOnly
                ? bookingUserRepository.findActiveUsersWithAtLeastThreePickups(ACTIVE_BOOKING_STATUSES, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                : bookingUserRepository.findAllActive(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<BookingUser> users = bookingUsers.getContent();
        Map<UUID, List<Slot>> bookingsByUser = groupBookings(users);
        Map<UUID, List<BookingComment>> commentsByUser = groupComments(users);
        return new AdminUsersView(bookingUsers, bookingsByUser, commentsByUser);
    }

    @Transactional
    public BookingUser disableBookingUser(UUID bookingUserId) {
        return bookingUserService.disable(bookingUserId);
    }

    private Map<UUID, List<Slot>> groupBookings(Collection<BookingUser> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Slot> bookings = slotRepository.findAllByActiveBookingUsersAndStatuses(users, ACTIVE_BOOKING_STATUSES);
        return bookings.stream().collect(Collectors.groupingBy(slot -> slot.getBookingUser().getId()));
    }

    private Map<UUID, List<BookingComment>> groupComments(Collection<BookingUser> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<BookingComment> comments = bookingCommentRepository.findAllByBookingUserInOrderByCreatedAtAsc(users);
        return comments.stream().collect(Collectors.groupingBy(comment -> comment.getBookingUser().getId()));
    }

    public record AdminUsersView(Page<BookingUser> users,
                                 Map<UUID, List<Slot>> bookingsByUser,
                                 Map<UUID, List<BookingComment>> commentsByUser) {
    }
}
