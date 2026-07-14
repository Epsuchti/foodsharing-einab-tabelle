package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.User;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingCommentRepository extends JpaRepository<BookingComment, UUID> {

    @EntityGraph(attributePaths = {"teacher", "teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    List<BookingComment> findAllByBookingUserOrderByCreatedAtAsc(User bookingUser);

    @EntityGraph(attributePaths = {"teacher", "teacher.bezirk", "bookingUser", "bookingUser.bezirk"})
    List<BookingComment> findAllByBookingUserInOrderByCreatedAtAsc(Collection<User> bookingUsers);
}
