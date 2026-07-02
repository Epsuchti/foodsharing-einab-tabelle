package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_store_pickups_cache", uniqueConstraints = @UniqueConstraint(name = "uk_foodsharing_store_pickups_cache_connection_store", columnNames = {"admin_connection_id", "store_id"}))
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingStorePickupsCache extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_connection_id", nullable = false)
    private FoodsharingAdminConnection adminConnection;

    @Column(nullable = false)
    private long storeId;

    @Column(nullable = false)
    private Instant refreshedAt;

    @Lob
    @Column(nullable = false)
    private String payloadJson;
}
