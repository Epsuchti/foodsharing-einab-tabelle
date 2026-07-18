package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomationAudit;
import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingRequestAutomationAuditRepository extends JpaRepository<FoodsharingRequestAutomationAudit, UUID> {
    List<FoodsharingRequestAutomationAudit> findTop100ByOrderByCreatedAtDesc();
    List<FoodsharingRequestAutomationAudit> findTop100ByAutomationAdminConnectionOrderByCreatedAtDesc(FoodsharingAdminConnection adminConnection);
    void deleteAllByAutomation(FoodsharingRequestAutomation automation);
    void deleteAllByCreatedAtBefore(java.time.Instant createdAt);
}
