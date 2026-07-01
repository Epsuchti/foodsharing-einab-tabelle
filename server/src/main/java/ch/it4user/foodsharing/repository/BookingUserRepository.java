package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface BookingUserRepository extends JpaRepository<BookingUser, UUID> {

    Optional<BookingUser> findByFoodsharingIdIgnoreCaseAndActiveTrue(String foodsharingId);

    Optional<BookingUser> findByFoodsharingIdIgnoreCase(String foodsharingId);

    List<BookingUser> findAllByActiveTrueOrderByCreatedAtDesc();

    @Query("""
        select bu from BookingUser bu
        where bu.active = true
        order by bu.createdAt desc
        """)
    Page<BookingUser> findAllActive(Pageable pageable);

    @Query(value = """
        select bu from BookingUser bu
        where bu.active = true
          and (
            select count(s) from Slot s
            where s.bookingUser = bu and s.status in :statuses
          ) >= 3
        order by bu.createdAt desc
        """,
            countQuery = """
        select count(bu) from BookingUser bu
        where bu.active = true
          and (
            select count(s) from Slot s
            where s.bookingUser = bu and s.status in :statuses
          ) >= 3
        """)
    Page<BookingUser> findActiveUsersWithAtLeastThreePickups(@Param("statuses") Collection<SlotStatus> statuses,
                                                             Pageable pageable);

    boolean existsByFoodsharingIdIgnoreCaseAndActiveTrue(String foodsharingId);
}
