package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EinAbRepository extends JpaRepository<EinAb, UUID> {

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    java.util.Optional<EinAb> findWithTeacherById(UUID id);

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    java.util.Optional<EinAb> findWithTeacherByIdAndBezirk(UUID id, Bezirk bezirk);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    java.util.Optional<EinAb> findWithTeacherForUpdateByIdAndBezirk(UUID id, Bezirk bezirk);

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    @Query("""
        select e from EinAb e
        where e.teacher = :teacher
          and e.bezirk = :bezirk
          and ((:pastOnly = true and e.startDateTime < current_timestamp)
            or (:pastOnly = false and e.startDateTime >= current_timestamp))
        order by case when :pastOnly = true then e.startDateTime else null end desc,
                 case when :pastOnly = false then e.startDateTime else null end asc
        """)
    Page<EinAb> findAllByTeacherAndBezirk(
            @Param("teacher") User teacher,
            @Param("bezirk") Bezirk bezirk,
            @Param("pastOnly") boolean pastOnly,
            Pageable pageable);

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    Page<EinAb> findAllByBezirkOrderByStartDateTimeAsc(Bezirk bezirk, Pageable pageable);
}
