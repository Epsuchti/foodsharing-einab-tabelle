package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.AuthSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession> findByTokenHash(String tokenHash);

    List<AuthSession> findAllByEmailIgnoreCase(String email);

    long deleteAllByEmailIgnoreCase(String email);
}
