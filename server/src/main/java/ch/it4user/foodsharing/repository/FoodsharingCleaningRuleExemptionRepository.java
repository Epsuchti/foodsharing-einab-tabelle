package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.FoodsharingCleaningRuleExemption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodsharingCleaningRuleExemptionRepository extends JpaRepository<FoodsharingCleaningRuleExemption, UUID> {
    List<FoodsharingCleaningRuleExemption> findAllByOrderByFoodsharingIdAsc();
    Optional<FoodsharingCleaningRuleExemption> findByFoodsharingId(String foodsharingId);
    boolean existsByFoodsharingId(String foodsharingId);
}
