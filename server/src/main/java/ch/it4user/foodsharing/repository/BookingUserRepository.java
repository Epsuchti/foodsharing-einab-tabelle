package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingUserRepository extends JpaRepository<BookingUser, UUID> {

    Optional<BookingUser> findByEmailIgnoreCaseAndFoodsharingIdIgnoreCase(String email, String foodsharingId);

    List<BookingUser> findAllByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
