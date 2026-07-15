package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EinAbRepository extends JpaRepository<EinAb, UUID> {

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    java.util.Optional<EinAb> findWithTeacherById(UUID id);

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    java.util.Optional<EinAb> findWithTeacherByIdAndBezirk(UUID id, Bezirk bezirk);

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    Page<EinAb> findAllByTeacherAndBezirkOrderByStartDateTimeAsc(User teacher, Bezirk bezirk, Pageable pageable);

    @EntityGraph(attributePaths = {"bezirk", "teacher", "teacher.bezirk"})
    Page<EinAb> findAllByBezirkOrderByStartDateTimeAsc(Bezirk bezirk, Pageable pageable);
}
