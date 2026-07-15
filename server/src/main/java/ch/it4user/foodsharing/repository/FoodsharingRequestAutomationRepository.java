package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingRequestAutomationRepository extends JpaRepository<FoodsharingRequestAutomation, UUID> {
    List<FoodsharingRequestAutomation> findAllByEnabledTrue();
    Optional<FoodsharingRequestAutomation> findByStoreId(long storeId);
    void deleteAllByAdminConnection(FoodsharingAdminConnection adminConnection);
}
