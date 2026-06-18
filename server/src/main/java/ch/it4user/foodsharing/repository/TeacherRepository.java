package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Teacher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByFoodsharingIdIgnoreCase(String foodsharingId);

    Optional<Teacher> findByEmailIgnoreCase(String email);
}
