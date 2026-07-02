package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingFuturePickupUsersCache;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingFuturePickupUsersCacheRepository extends JpaRepository<FoodsharingFuturePickupUsersCache, UUID> {
    Optional<FoodsharingFuturePickupUsersCache> findByAdminConnection(FoodsharingAdminConnection adminConnection);
}
