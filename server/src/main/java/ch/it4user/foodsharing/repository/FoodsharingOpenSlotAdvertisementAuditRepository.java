package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAudit;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingOpenSlotAdvertisementAuditRepository extends JpaRepository<FoodsharingOpenSlotAdvertisementAudit, UUID> {
    java.util.List<FoodsharingOpenSlotAdvertisementAudit> findTop100ByOrderByCreatedAtDesc();
    boolean existsByAutomationAndPickupDateAndTriggerHoursBefore(FoodsharingOpenSlotAdvertisementAutomation automation, Instant pickupDate, int triggerHoursBefore);
    java.util.List<FoodsharingOpenSlotAdvertisementAudit> findAllByAutomationAndPickupDateAndTelegramMessageIdIsNotNullAndTelegramDeletedAtIsNull(FoodsharingOpenSlotAdvertisementAutomation automation, Instant pickupDate);
    void deleteAllByAutomation(FoodsharingOpenSlotAdvertisementAutomation automation);
}
