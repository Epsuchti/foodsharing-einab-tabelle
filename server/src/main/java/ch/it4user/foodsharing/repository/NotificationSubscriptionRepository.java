package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, UUID> {

    @EntityGraph(attributePaths = "bezirk")
    Optional<NotificationSubscription> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "bezirk")
    Optional<NotificationSubscription> findByBezirkAndEmailIgnoreCase(Bezirk bezirk, String email);

    @EntityGraph(attributePaths = "bezirk")
    Optional<NotificationSubscription> findByUnsubscribeToken(String unsubscribeToken);

    @Override
    @EntityGraph(attributePaths = "bezirk")
    Optional<NotificationSubscription> findById(UUID id);

    @EntityGraph(attributePaths = "bezirk")
    List<NotificationSubscription> findAllByBezirkAndActiveTrueOrderByCreatedAtAsc(Bezirk bezirk);
}
