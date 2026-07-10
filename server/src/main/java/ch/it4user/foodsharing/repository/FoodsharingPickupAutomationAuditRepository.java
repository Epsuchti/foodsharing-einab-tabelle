package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingPickupAutomationAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingPickupAutomationAuditRepository extends JpaRepository<FoodsharingPickupAutomationAudit, UUID> {
    List<FoodsharingPickupAutomationAudit> findTop100ByAdminConnectionOrderByCreatedAtDesc(FoodsharingAdminConnection adminConnection);
    List<FoodsharingPickupAutomationAudit> findTop100ByOrderByCreatedAtDesc();
    void deleteAllByAdminConnection(FoodsharingAdminConnection adminConnection);
}
