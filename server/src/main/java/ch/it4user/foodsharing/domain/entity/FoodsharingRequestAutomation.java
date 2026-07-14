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
@Table(name = "foodsharing_request_automations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_foodsharing_request_automations_store", columnNames = {"store_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingRequestAutomation extends BaseEntity {
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
    private boolean dryRunEnabled = true;

    @Column(name = "distance_rule_enabled", nullable = false)
    private boolean distanceRuleEnabled = false;

    @Column(name = "maximum_distance_km", nullable = false)
    private double maximumDistanceKm = 0.0;
}
