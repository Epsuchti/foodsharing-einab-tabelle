package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingOpenSlotAdvertisementAutomation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingOpenSlotAdvertisementAutomationRepository extends JpaRepository<FoodsharingOpenSlotAdvertisementAutomation, UUID> {
    List<FoodsharingOpenSlotAdvertisementAutomation> findAllByEnabledTrue();
    List<FoodsharingOpenSlotAdvertisementAutomation> findAllByStoreIdOrderByAdvertNumberAsc(long storeId);
    Optional<FoodsharingOpenSlotAdvertisementAutomation> findByStoreIdAndAdvertNumber(long storeId, int advertNumber);
    void deleteAllByAdminConnection(FoodsharingAdminConnection adminConnection);
}
