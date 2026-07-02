package ch.it4user.foodsharing.domain.entity;

import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "slots")
@Getter
@Setter
@NoArgsConstructor
public class Slot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "einab_id", nullable = false)
    private EinAb einAb;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_user_id")
    private BookingUser bookingUser;

    @Column
    private Instant bookedAt;

    @Column(length = 64)
    private String pendingConfirmationTokenHash;

    @Column
    private Instant pendingConfirmationExpiresAt;

    @Column
    private Instant doneAt;

}
