package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_request_automation_audits")
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingRequestAutomationAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "automation_id", nullable = false)
    private FoodsharingRequestAutomation automation;

    @Column(nullable = false)
    private long storeId;

    @Column(nullable = false, length = 255)
    private String storeName = "";

    @Column(nullable = false, length = 100)
    private String foodsharingUserId;

    @Column(length = 255)
    private String foodsharingUserName;

    @Column(nullable = false)
    private boolean dryRun;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(length = 32600)
    private String message;

    @Column(length = 32600)
    private String error;
}
