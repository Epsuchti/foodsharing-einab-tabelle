package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.SlotComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotCommentRepository extends JpaRepository<SlotComment, UUID> {

    @EntityGraph(attributePaths = {"teacher"})
    List<SlotComment> findAllBySlotOrderByCreatedAtAsc(Slot slot);
}
