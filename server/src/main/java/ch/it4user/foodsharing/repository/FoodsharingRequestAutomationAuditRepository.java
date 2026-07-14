package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomationAudit;
import ch.it4user.foodsharing.domain.entity.FoodsharingRequestAutomation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingRequestAutomationAuditRepository extends JpaRepository<FoodsharingRequestAutomationAudit, UUID> {
    List<FoodsharingRequestAutomationAudit> findTop100ByOrderByCreatedAtDesc();
    void deleteAllByAutomation(FoodsharingRequestAutomation automation);
}
