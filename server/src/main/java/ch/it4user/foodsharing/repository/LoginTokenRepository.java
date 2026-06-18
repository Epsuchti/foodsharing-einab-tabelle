package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.LoginToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginTokenRepository extends JpaRepository<LoginToken, UUID> {

    Optional<LoginToken> findByTokenHash(String tokenHash);
}
