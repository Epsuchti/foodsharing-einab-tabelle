package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingStorePickupsCache;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingStorePickupsCacheRepository extends JpaRepository<FoodsharingStorePickupsCache, UUID> {
    Optional<FoodsharingStorePickupsCache> findByAdminConnectionAndStoreId(FoodsharingAdminConnection adminConnection, long storeId);
}
