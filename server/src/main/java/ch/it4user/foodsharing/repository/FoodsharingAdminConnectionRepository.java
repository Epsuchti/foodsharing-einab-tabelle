package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingAdminConnectionRepository extends JpaRepository<FoodsharingAdminConnection, UUID> {
    Optional<FoodsharingAdminConnection> findByAdminUser(User adminUser);
}
