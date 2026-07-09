package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFoodsharingIdIgnoreCaseAndActiveTrue(String foodsharingId);

    Optional<User> findByFoodsharingIdIgnoreCase(String foodsharingId);

    @Query("""
        select u from User u
        where u.active = true
          and u.canGiveEinAbs = false
          and u.canManageUsers = false
        order by u.createdAt desc
        """)
    List<User> findAllActiveBookingUsersOrderByCreatedAtDesc();

    Page<User> findAllByCanManageUsersTrueOrderByNameAsc(Pageable pageable);

    Page<User> findAllByCanGiveEinAbsTrueOrWantsToBeTeacherTrueOrderByNameAsc(Pageable pageable);

    boolean existsByFoodsharingIdIgnoreCaseAndActiveTrue(String foodsharingId);

    @Query("""
        select u from User u
        where u.active = true
        order by u.createdAt desc
        """)
    Page<User> findAllActive(Pageable pageable);

    @Query(value = """
        select u from User u
        where u.active = true
          and (
            select count(s) from Slot s
            where s.bookingUser = u and s.status in :statuses
          ) >= 3
        order by u.createdAt desc
        """,
            countQuery = """
        select count(u) from User u
        where u.active = true
          and (
            select count(s) from Slot s
            where s.bookingUser = u and s.status in :statuses
          ) >= 3
        """)
    Page<User> findActiveUsersWithAtLeastThreePickups(@Param("statuses") Collection<SlotStatus> statuses,
                                                      Pageable pageable);
}
