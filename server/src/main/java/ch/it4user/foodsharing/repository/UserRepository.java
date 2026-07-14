package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.User;
import jakarta.persistence.LockModeType;
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

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"bezirk"})
    Optional<User> findByFoodsharingIdIgnoreCaseAndActiveTrue(String foodsharingId);

    @EntityGraph(attributePaths = {"bezirk"})
    Optional<User> findByFoodsharingIdIgnoreCase(String foodsharingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.foodsharingId) = lower(:foodsharingId)")
    Optional<User> findByFoodsharingIdIgnoreCaseForUpdate(@Param("foodsharingId") String foodsharingId);

    @EntityGraph(attributePaths = {"bezirk"})
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithBezirkById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"bezirk"})
    List<User> findAllByBezirk(Bezirk bezirk);

    @EntityGraph(attributePaths = {"bezirk"})
    List<User> findAllByBezirkIsNull();

    @EntityGraph(attributePaths = {"bezirk"})
    Page<User> findAllByCanManageUsersTrueOrderByNameAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"bezirk"})
    @Query("""
        select u from User u
        where u.bezirk = :bezirk
          and (u.canGiveEinAbs = true or u.wantsToBeTeacher = true)
        order by u.name asc
        """)
    Page<User> findAllTeachersByBezirk(@Param("bezirk") Bezirk bezirk, Pageable pageable);

}
