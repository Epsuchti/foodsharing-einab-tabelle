package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Length;

@Entity
@Table(name = "foodsharing_future_pickup_users_cache", uniqueConstraints = @UniqueConstraint(
        name = "uk_foodsharing_future_pickup_users_cache_connection_bezirk",
        columnNames = {"admin_connection_id", "bezirk_id"}))
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingFuturePickupUsersCache extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_connection_id", nullable = false)
    private FoodsharingAdminConnection adminConnection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bezirk_id", nullable = false)
    private Bezirk bezirk;

    @Column(nullable = false)
    private Instant refreshedAt;

    @Column(nullable = false, length = Length.LONG32)
    private String payloadJson;
}
