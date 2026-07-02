package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreMembersCache;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingStoreMembersCacheRepository extends JpaRepository<FoodsharingStoreMembersCache, UUID> {
    Optional<FoodsharingStoreMembersCache> findByAdminConnectionAndStoreId(FoodsharingAdminConnection adminConnection, long storeId);
}
