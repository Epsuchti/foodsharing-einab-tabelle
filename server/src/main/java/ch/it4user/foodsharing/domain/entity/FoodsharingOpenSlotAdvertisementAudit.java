package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_open_slot_advertisement_audits")
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingOpenSlotAdvertisementAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "automation_id", nullable = false)
    private FoodsharingOpenSlotAdvertisementAutomation automation;

    @Column(nullable = false)
    private long storeId;

    @Column(nullable = false)
    private Instant pickupDate;

    @Column(nullable = false)
    private int triggerHoursBefore;

    @Column
    private Integer telegramMessageId;

    @Column
    private Instant telegramDeletedAt;

    @Column(nullable = false, length = 32)
    private String status = "SENT";

    @Column(nullable = false)
    private boolean dryRun = false;

    @Column(length = 32600)
    private String message;

    @Column(length = 1000)
    private String reason;

    @Column(length = 32600)
    private String error;
}
