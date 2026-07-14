package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_cleaning_rule_exemptions", uniqueConstraints =
        @UniqueConstraint(name = "uk_foodsharing_cleaning_exemptions_bezirk_user", columnNames = {"bezirk_id", "foodsharing_id"}))
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingCleaningRuleExemption extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bezirk_id", nullable = false)
    private Bezirk bezirk;

    @Column(nullable = false, length = 100)
    private String foodsharingId;

    @Column(nullable = false, length = 1000)
    private String reason;
}
