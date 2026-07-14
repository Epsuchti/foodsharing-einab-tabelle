package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.FoodsharingAdminConnection;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingStoreAutomationRepository extends JpaRepository<FoodsharingStoreAutomation, UUID> {
    List<FoodsharingStoreAutomation> findAllByBezirk(Bezirk bezirk);
    List<FoodsharingStoreAutomation> findAllByBezirkAndCleaningRuleEnabledTrue(Bezirk bezirk);
    List<FoodsharingStoreAutomation> findAllByEnabledTrue();
    List<FoodsharingStoreAutomation> findAllByBezirkAndEnabledTrue(Bezirk bezirk);
    Optional<FoodsharingStoreAutomation> findByBezirkAndStoreId(Bezirk bezirk, long storeId);
    Optional<FoodsharingStoreAutomation> findByStoreId(long storeId);
    void deleteAllByAdminConnection(FoodsharingAdminConnection adminConnection);
}
