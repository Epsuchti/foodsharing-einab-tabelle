package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingStoreAutomationRepository extends JpaRepository<FoodsharingStoreAutomation, UUID> {
    List<FoodsharingStoreAutomation> findAllByAdminConnection(FoodsharingAdminConnection adminConnection);
    List<FoodsharingStoreAutomation> findAllByEnabledTrue();
    Optional<FoodsharingStoreAutomation> findByAdminConnectionAndStoreId(FoodsharingAdminConnection adminConnection, long storeId);
    Optional<FoodsharingStoreAutomation> findByStoreId(long storeId);
}
