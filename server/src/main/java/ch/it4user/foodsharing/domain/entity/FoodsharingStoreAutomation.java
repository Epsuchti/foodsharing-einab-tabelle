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
@Table(name = "foodsharing_store_automations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_foodsharing_store_automations_connection_store", columnNames = {"admin_connection_id", "store_id"}),
        @UniqueConstraint(name = "uk_foodsharing_store_automations_store", columnNames = {"store_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingStoreAutomation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_connection_id", nullable = false)
    private FoodsharingAdminConnection adminConnection;

    @Column(name = "store_id", nullable = false)
    private long storeId;

    @Column(nullable = false, length = 255)
    private String storeName = "";

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private boolean gapRuleEnabled = false;

    @Column(nullable = false)
    private int minimumGapDays = 0;

    @Column(nullable = false)
    private boolean cleaningRuleEnabled = false;

    @Column(nullable = false)
    private boolean experienceRuleEnabled = false;
}
