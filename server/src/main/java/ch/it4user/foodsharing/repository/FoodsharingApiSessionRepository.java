package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingApiSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingApiSessionRepository extends JpaRepository<FoodsharingApiSession, UUID> {
}
