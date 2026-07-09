package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_cleaning_rule_exemptions")
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingCleaningRuleExemption extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String foodsharingId;

    @Column(nullable = false, length = 1000)
    private String reason;
}
