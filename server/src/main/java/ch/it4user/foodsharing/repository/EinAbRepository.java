package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Teacher;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EinAbRepository extends JpaRepository<EinAb, UUID> {

    @EntityGraph(attributePaths = {"teacher"})
    List<EinAb> findAllByTeacherOrderByStartDateTimeAsc(Teacher teacher);

    @EntityGraph(attributePaths = {"teacher"})
    List<EinAb> findAllByOrderByStartDateTimeAsc();

    @EntityGraph(attributePaths = {"teacher"})
    Page<EinAb> findAllByTeacherOrderByStartDateTimeAsc(Teacher teacher, Pageable pageable);

    @EntityGraph(attributePaths = {"teacher"})
    Page<EinAb> findAllByOrderByStartDateTimeAsc(Pageable pageable);
}
