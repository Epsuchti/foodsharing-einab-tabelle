package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.FoodsharingCleaningRuleExemption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingCleaningRuleExemptionRepository extends JpaRepository<FoodsharingCleaningRuleExemption, UUID> {
    List<FoodsharingCleaningRuleExemption> findAllByBezirkOrderByFoodsharingIdAsc(Bezirk bezirk);
    Optional<FoodsharingCleaningRuleExemption> findByBezirkAndFoodsharingId(Bezirk bezirk, String foodsharingId);
    Optional<FoodsharingCleaningRuleExemption> findByIdAndBezirk(UUID id, Bezirk bezirk);
    boolean existsByBezirkAndFoodsharingId(Bezirk bezirk, String foodsharingId);
}
