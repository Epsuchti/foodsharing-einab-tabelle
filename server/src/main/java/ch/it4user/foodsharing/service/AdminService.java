package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.BookingCommentRepository;
import ch.it4user.foodsharing.repository.UserRepository;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final List<SlotStatus> ACTIVE_BOOKING_STATUSES = List.of(SlotStatus.BOOKED, SlotStatus.DONE);

    private final TeacherService teacherService;
    private final BookingUserService bookingUserService;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final BookingCommentRepository bookingCommentRepository;

    public AdminService(TeacherService teacherService,
                        BookingUserService bookingUserService,
                        SlotRepository slotRepository,
                        UserRepository userRepository,
                        BookingCommentRepository bookingCommentRepository) {
        this.teacherService = teacherService;
        this.bookingUserService = bookingUserService;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.bookingCommentRepository = bookingCommentRepository;
    }

    public Page<User> getTeachers(int page, int size) {
        return teacherService.findAllTeachers(page, size);
    }

    public Page<User> getAdmins(int page, int size) {
        return userRepository.findAllByCanManageUsersTrueOrderByNameAsc(
                org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    public User setTeacherActive(java.util.UUID teacherId, boolean active) {
        return teacherService.setTeacherActive(teacherId, active);
    }

    public User setTeacherAdmin(UUID teacherId, boolean admin) {
        return teacherService.setTeacherAdmin(teacherId, admin);
    }

    public Page<EinAb> getEinAbs(int page, int size) {
        return teacherService.findAllEinAbs(page, size);
    }

    public Page<Slot> getBookings(int page, int size) {
        return slotRepository.findAllByStatusInOrderByEinAbStartDateTimeAsc(
                ACTIVE_BOOKING_STATUSES,
                org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    public AdminUsersView getUsers(int page, int size, boolean threePickupsOnly, boolean activeOnly) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<User> users = userRepository.findAll();
        Map<UUID, List<Slot>> bookingsByUser = groupBookings(users);
        Map<UUID, List<BookingComment>> commentsByUser = groupComments(users);
        List<User> sortedUsers = users.stream()
                .filter(user -> !activeOnly || user.isActive())
                .filter(user -> !threePickupsOnly || bookingsByUser.getOrDefault(user.getId(), List.of()).size() >= 3)
                .sorted(Comparator
                        .comparing((User user) -> latestPickupAt(bookingsByUser.getOrDefault(user.getId(), List.of())),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(User::getCreatedAt, Comparator.reverseOrder()))
                .toList();
        int fromIndex = Math.min(safePage * safeSize, sortedUsers.size());
        int toIndex = Math.min(fromIndex + safeSize, sortedUsers.size());
        Page<User> bookingUsers = new org.springframework.data.domain.PageImpl<>(
                sortedUsers.subList(fromIndex, toIndex),
                org.springframework.data.domain.PageRequest.of(safePage, safeSize),
                sortedUsers.size());
        return new AdminUsersView(bookingUsers, bookingsByUser, commentsByUser);
    }

    @Transactional
    public User disableBookingUser(UUID bookingUserId) {
        return bookingUserService.disable(bookingUserId);
    }

    @Transactional
    public User enableBookingUser(UUID bookingUserId) {
        return bookingUserService.enable(bookingUserId);
    }

    @Transactional
    public User grantBookingUserAdmin(UUID bookingUserId) {
        User bookingUser = userRepository.findById(bookingUserId)
                .filter(User::isActive)
                .filter(user -> !user.isCanGiveEinAbs())
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
        bookingUser.setCanManageUsers(true);
        bookingUser.setActive(true);
        return bookingUser;
    }

    @Transactional
    public User setAdminActive(UUID adminUserId, boolean active) {
        User adminUser = userRepository.findById(adminUserId)
                .filter(User::isCanManageUsers)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND));
        if (adminUser.isCanGiveEinAbs() || adminUser.isWantsToBeTeacher()) {
            return teacherService.setTeacherActive(adminUserId, active);
        }
        adminUser.setActive(active);
        return adminUser;
    }

    @Transactional
    public User setUserPermissions(UUID userId, UserPermissions permissions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND));
        user.setCanGiveEinAbs(permissions.canGiveEinAbs());
        user.setCanManageUsers(permissions.canManageUsers());
        user.setCanUseAutomations(permissions.canUseAutomations());
        user.setCanSeeUserPickupCountGrouping(permissions.canSeeUserPickupCountGrouping());
        user.setCanUseAutomationSlotApproval(permissions.canUseAutomationSlotApproval());
        user.setCanSeeAllAutomationDecisions(permissions.canSeeAllAutomationDecisions());
        if (permissions.canManageUsers()) {
            user.setActive(true);
        }
        return user;
    }

    @Transactional
    public User setAdminAdmin(UUID adminUserId, boolean admin) {
        User adminUser = userRepository.findById(adminUserId)
                .filter(User::isCanManageUsers)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND));
        adminUser.setCanManageUsers(admin);
        if (admin) {
            adminUser.setActive(true);
        }
        return adminUser;
    }

    private Map<UUID, List<Slot>> groupBookings(Collection<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Slot> bookings = slotRepository.findAllByActiveBookingUsersAndStatuses(users, ACTIVE_BOOKING_STATUSES);
        return bookings.stream().collect(Collectors.groupingBy(slot -> slot.getBookingUser().getId()));
    }

    private Map<UUID, List<BookingComment>> groupComments(Collection<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<BookingComment> comments = bookingCommentRepository.findAllByBookingUserInOrderByCreatedAtAsc(users);
        return comments.stream().collect(Collectors.groupingBy(comment -> comment.getBookingUser().getId()));
    }

    private java.time.Instant latestPickupAt(List<Slot> bookings) {
        return bookings.stream()
                .map(slot -> slot.getEinAb().getStartDateTime())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public record UserPermissions(boolean canGiveEinAbs, boolean canManageUsers, boolean canUseAutomations,
                                  boolean canSeeUserPickupCountGrouping, boolean canUseAutomationSlotApproval,
                                  boolean canSeeAllAutomationDecisions) {
    }

    public record AdminUsersView(Page<User> users,
                                 Map<UUID, List<Slot>> bookingsByUser,
                                 Map<UUID, List<BookingComment>> commentsByUser) {
    }
}
