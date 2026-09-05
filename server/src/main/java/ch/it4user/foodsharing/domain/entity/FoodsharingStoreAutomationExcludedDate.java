package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_store_automation_excluded_dates", uniqueConstraints =
        @UniqueConstraint(name = "uk_foodsharing_store_automation_excluded_dates", columnNames = {"automation_id", "excluded_date"}))
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingStoreAutomationExcludedDate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "automation_id", nullable = false)
    private FoodsharingStoreAutomation automation;

    @Column(name = "excluded_date", nullable = false)
    private LocalDate excludedDate;
}
