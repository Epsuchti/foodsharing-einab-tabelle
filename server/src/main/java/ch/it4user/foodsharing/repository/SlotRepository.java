package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher"})
    @Query("""
        select s from Slot s
        where s.status = ch.it4user.foodsharing.domain.enumtype.SlotStatus.AVAILABLE
          and (:category is null or s.einAb.category = :category)
          and (:visitFairteiler is null or s.einAb.visitFairteiler = :visitFairteiler)
          and (
            :searchPattern is null
            or lower(s.einAb.teacher.name) like :searchPattern
            or lower(coalesce(s.einAb.publicLocation, '')) like :searchPattern
            or lower(coalesce(s.einAb.location, '')) like :searchPattern
          )
        order by s.einAb.startDateTime asc
        """)
    Page<Slot> findAvailableSlots(@Param("searchPattern") String searchPattern,
                                  @Param("category") EinAbCategory category,
                                  @Param("visitFairteiler") Boolean visitFairteiler,
                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findForUpdateById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    List<Slot> findAllByEinAbInOrderByEinAbStartDateTimeAsc(Collection<EinAb> einAbs);

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    @Query("""
        select s from Slot s
        where s.status in :statuses
        order by s.einAb.startDateTime asc
        """)
    Page<Slot> findAllByStatusInOrderByEinAbStartDateTimeAsc(@Param("statuses") Collection<SlotStatus> statuses,
                                                             Pageable pageable);

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    @Query("""
        select s from Slot s
        where s.bookingUser in :users
          and s.status in :statuses
        order by s.einAb.startDateTime desc
        """)
    List<Slot> findAllByBookingUsersAndStatuses(@Param("users") Collection<BookingUser> users,
                                                @Param("statuses") Collection<SlotStatus> statuses);

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    @Query("""
        select s from Slot s
        where s.bookingUser in :users
          and s.status in :statuses
          and s.bookingUser.active = true
        order by s.einAb.startDateTime desc
        """)
    List<Slot> findAllByActiveBookingUsersAndStatuses(@Param("users") Collection<BookingUser> users,
                                                      @Param("statuses") Collection<SlotStatus> statuses);

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    @Query("""
        select s from Slot s
        where s.bookingUser = :bookingUser
          and s.status in :statuses
        order by s.einAb.startDateTime desc
        """)
    Page<Slot> findAllByBookingUserAndStatuses(@Param("bookingUser") BookingUser bookingUser,
                                               @Param("statuses") Collection<SlotStatus> statuses,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {"einAb", "einAb.teacher", "bookingUser"})
    @Query("""
        select s from Slot s
        where s.einAb.teacher = :teacher
          and s.status in :statuses
        order by s.einAb.startDateTime desc
        """)
    Page<Slot> findAllByTeacherAndStatuses(@Param("teacher") Teacher teacher,
                                           @Param("statuses") Collection<SlotStatus> statuses,
                                           Pageable pageable);

    long countByBookingUserAndStatusIn(BookingUser bookingUser, Collection<SlotStatus> statuses);

    boolean existsByBookingUserAndStatusInAndEinAbTeacher(BookingUser bookingUser,
                                                           Collection<SlotStatus> statuses,
                                                           Teacher teacher);

    boolean existsByBookingUserAndStatusInAndEinAbCategory(BookingUser bookingUser,
                                                           Collection<SlotStatus> statuses,
                                                           EinAbCategory category);

    long countByBookingUserAndStatus(BookingUser bookingUser, SlotStatus status);

    boolean existsByEinAbAndStatusIn(EinAb einAb, Collection<SlotStatus> statuses);

    List<Slot> findAllByEinAbOrderByCreatedAtAsc(EinAb einAb);

    Optional<Slot> findByPendingConfirmationTokenHash(String tokenHash);

    List<Slot> findAllByStatusAndPendingConfirmationExpiresAtBefore(SlotStatus status, java.time.Instant expiresAt);
}
