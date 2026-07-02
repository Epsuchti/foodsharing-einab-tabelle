package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Teacher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    boolean existsByFoodsharingIdIgnoreCase(String foodsharingId);

    Optional<Teacher> findByFoodsharingIdIgnoreCase(String foodsharingId);

    Page<Teacher> findAllByTeacherTrueOrderByNameAsc(Pageable pageable);
}
