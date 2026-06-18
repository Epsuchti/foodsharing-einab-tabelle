package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, UUID> {

    Optional<NotificationSubscription> findByEmailIgnoreCase(String email);

    Optional<NotificationSubscription> findByUnsubscribeToken(String unsubscribeToken);

    List<NotificationSubscription> findAllByActiveTrueOrderByCreatedAtAsc();
}
