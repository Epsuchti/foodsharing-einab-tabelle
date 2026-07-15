package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAudit;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodsharingOpenSlotAdvertisementAuditRepository extends JpaRepository<FoodsharingOpenSlotAdvertisementAudit, UUID> {
    java.util.List<FoodsharingOpenSlotAdvertisementAudit> findTop100ByOrderByCreatedAtDesc();
    @Query("select count(a) > 0 from FoodsharingOpenSlotAdvertisementAudit a "
            + "where a.automation = :automation and a.pickupDate = :pickupDate "
            + "and a.triggerHoursBefore = :triggerHoursBefore")
    boolean existsByAutomationAndPickupDateAndTriggerHoursBefore(
            @Param("automation") FoodsharingOpenSlotAdvertisementAutomation automation,
            @Param("pickupDate") Instant pickupDate,
            @Param("triggerHoursBefore") int triggerHoursBefore);
    java.util.List<FoodsharingOpenSlotAdvertisementAudit> findAllByAutomationAndPickupDateAndTelegramMessageIdIsNotNullAndTelegramDeletedAtIsNull(FoodsharingOpenSlotAdvertisementAutomation automation, Instant pickupDate);
    void deleteAllByAutomation(FoodsharingOpenSlotAdvertisementAutomation automation);
    void deleteAllByCreatedAtBefore(Instant createdAt);
}
