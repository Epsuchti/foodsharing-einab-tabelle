package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.User;
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

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk"})
    @Query("""
        select s from Slot s
        where s.status = ch.it4user.foodsharing.domain.enumtype.SlotStatus.AVAILABLE
          and s.einAb.bezirk = :bezirk
          and s.einAb.startDateTime > current_timestamp
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
    Page<Slot> findAvailableSlots(@Param("bezirk") Bezirk bezirk,
                                  @Param("searchPattern") String searchPattern,
                                  @Param("category") EinAbCategory category,
                                  @Param("visitFairteiler") Boolean visitFairteiler,
                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    @Query("""
        select s from Slot s
        where s.id = :id
          and exists (
            select e.id from EinAb e
            where e = s.einAb and e.bezirk = :bezirk
          )
        """)
    Optional<Slot> findForUpdateByIdAndBezirk(@Param("id") UUID id, @Param("bezirk") Bezirk bezirk);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    List<Slot> findAllByEinAbInOrderByEinAbStartDateTimeAsc(Collection<EinAb> einAbs);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    @Query("""
        select s from Slot s
        where s.status in :statuses
          and s.einAb.bezirk = :bezirk
        order by s.einAb.startDateTime asc
        """)
    Page<Slot> findAllByStatusInAndBezirkOrderByEinAbStartDateTimeAsc(@Param("statuses") Collection<SlotStatus> statuses,
                                                                     @Param("bezirk") Bezirk bezirk,
                                                                     Pageable pageable);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    @Query("""
        select s from Slot s
        where s.bookingUser in :users
          and s.status in :statuses
          and s.bookingUser.active = true
          and s.einAb.bezirk = :bezirk
        order by s.einAb.startDateTime desc
        """)
    List<Slot> findAllByActiveBookingUsersAndStatusesAndBezirk(@Param("users") Collection<User> users,
                                                               @Param("statuses") Collection<SlotStatus> statuses,
                                                               @Param("bezirk") Bezirk bezirk);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    @Query("""
        select s from Slot s
        where s.bookingUser in :users
          and s.status in :statuses
          and s.bookingUser.active = true
        order by s.einAb.startDateTime desc
        """)
    List<Slot> findAllByActiveBookingUsersAndStatuses(@Param("users") Collection<User> users,
                                                      @Param("statuses") Collection<SlotStatus> statuses);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    @Query("""
        select s from Slot s
        where s.bookingUser = :bookingUser
          and s.status in :statuses
          and s.einAb.bezirk = :bezirk
        order by s.einAb.startDateTime desc
        """)
    Page<Slot> findAllByBookingUserAndStatusesAndBezirk(@Param("bookingUser") User bookingUser,
                                                        @Param("statuses") Collection<SlotStatus> statuses,
                                                        @Param("bezirk") Bezirk bezirk,
                                                        Pageable pageable);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    @Query("""
        select s from Slot s
        where s.einAb.teacher = :teacher
          and s.einAb.bezirk = :bezirk
          and s.status in :statuses
        order by s.einAb.startDateTime desc
        """)
    Page<Slot> findAllByTeacherAndStatusesAndBezirk(@Param("teacher") User teacher,
                                                    @Param("statuses") Collection<SlotStatus> statuses,
                                                    @Param("bezirk") Bezirk bezirk,
                                                    Pageable pageable);

    long countByBookingUserAndStatusAndEinAbBezirk(User bookingUser, SlotStatus status, Bezirk bezirk);

    @Query("""
        select (count(s) > 0) from Slot s
        where s.bookingUser = :bookingUser
          and s.status = ch.it4user.foodsharing.domain.enumtype.SlotStatus.PENDING_CONFIRMATION
          and s.einAb.bezirk <> :bezirk
          and s.pendingConfirmationExpiresAt > current_timestamp
        """)
    boolean existsOpenPendingConfirmationInDifferentBezirk(@Param("bookingUser") User bookingUser,
                                                            @Param("bezirk") Bezirk bezirk);

    boolean existsByBookingUserAndStatusInAndEinAbTeacherAndEinAbBezirk(User bookingUser,
                                                                        Collection<SlotStatus> statuses,
                                                                        User teacher,
                                                                        Bezirk bezirk);

    boolean existsByBookingUserAndStatusInAndEinAbCategoryAndEinAbBezirk(User bookingUser,
                                                                         Collection<SlotStatus> statuses,
                                                                         EinAbCategory category,
                                                                         Bezirk bezirk);

    boolean existsByEinAbAndStatusIn(EinAb einAb, Collection<SlotStatus> statuses);

    List<Slot> findAllByEinAbOrderByCreatedAtAsc(EinAb einAb);

    @EntityGraph(attributePaths = {"einAb", "einAb.bezirk", "einAb.teacher", "einAb.teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    Optional<Slot> findByPendingConfirmationTokenHash(String tokenHash);

    List<Slot> findAllByStatusAndPendingConfirmationExpiresAtBefore(SlotStatus status, java.time.Instant expiresAt);
}
