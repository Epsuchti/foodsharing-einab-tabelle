package ch.it4user.foodsharing.domain.entity;

import ch.it4user.foodsharing.domain.enumtype.FoodsharingPickupAutomationDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "foodsharing_pickup_automation_audits")
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingPickupAutomationAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bezirk_id", nullable = false)
    private Bezirk bezirk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_connection_id", nullable = false)
    private FoodsharingAdminConnection adminConnection;

    @Column(nullable = false)
    private long storeId;

    @Column(nullable = false, length = 100)
    private String foodsharingUserId;

    @Column(length = 255)
    private String foodsharingUserName;

    @Column(nullable = false)
    private Instant pickupDate;

    @Column(nullable = false)
    private boolean dryRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FoodsharingPickupAutomationDecision decision;

    @Column(nullable = false, length = 4000)
    private String reasons;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String userMessage;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String foodsharingError;
}
