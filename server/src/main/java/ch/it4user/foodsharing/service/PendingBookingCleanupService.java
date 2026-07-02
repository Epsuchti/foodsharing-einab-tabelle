package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingBookingCleanupService {
    private final SlotRepository slotRepository;

    public PendingBookingCleanupService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Scheduled(fixedDelayString = "${app.bookings.pending-cleanup-delay-ms:300000}")
    @Transactional
    public void releaseExpiredPendingBookings() {
        slotRepository.findAllByStatusAndPendingConfirmationExpiresAtBefore(SlotStatus.PENDING_CONFIRMATION, Instant.now())
                .forEach(slot -> {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slot.setBookingUser(null);
                    slot.setBookedAt(null);
                    slot.setPendingConfirmationTokenHash(null);
                    slot.setPendingConfirmationExpiresAt(null);
                });
    }
}
