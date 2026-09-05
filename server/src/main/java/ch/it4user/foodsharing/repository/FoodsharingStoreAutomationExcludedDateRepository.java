package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomation;
import ch.it4user.foodsharing.domain.entity.FoodsharingStoreAutomationExcludedDate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingStoreAutomationExcludedDateRepository extends JpaRepository<FoodsharingStoreAutomationExcludedDate, UUID> {
    List<FoodsharingStoreAutomationExcludedDate> findAllByAutomationBezirkOrderByAutomationStoreNameAscExcludedDateAsc(Bezirk bezirk);
    Optional<FoodsharingStoreAutomationExcludedDate> findByAutomationAndExcludedDate(FoodsharingStoreAutomation automation, LocalDate excludedDate);
    Optional<FoodsharingStoreAutomationExcludedDate> findByIdAndAutomationBezirk(UUID id, Bezirk bezirk);
    boolean existsByAutomationAndExcludedDate(FoodsharingStoreAutomation automation, LocalDate excludedDate);
    void deleteAllByAutomation(FoodsharingStoreAutomation automation);
}
