package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_open_slot_advertisement_automations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_foodsharing_open_slot_advertisement_automations_store_number", columnNames = {"store_id", "advert_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingOpenSlotAdvertisementAutomation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_connection_id", nullable = false)
    private FoodsharingAdminConnection adminConnection;

    @Column(name = "store_id", nullable = false)
    private long storeId;

    @Column(nullable = false, length = 255)
    private String storeName = "";

    @Column(name = "advert_number", nullable = false)
    private int advertNumber = 1;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "dry_run_enabled", nullable = false)
    private boolean dryRunEnabled = true;

    @Column(nullable = false)
    private int triggerHoursBefore = 24;

    @Column(nullable = false)
    private boolean sendToStoreChat = true;

    @Column(nullable = false)
    private boolean sendToTelegram = false;

    @Column(length = 255)
    private String telegramChatId;

    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "store_messages_json", nullable = false, columnDefinition = "TEXT")
    private String storeMessagesJson = "[]";

    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "telegram_messages_json", nullable = false, columnDefinition = "TEXT")
    private String telegramMessagesJson = "[]";
}
