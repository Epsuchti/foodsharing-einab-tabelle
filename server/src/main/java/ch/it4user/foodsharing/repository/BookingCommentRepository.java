package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.BookingComment;
import ch.it4user.foodsharing.domain.entity.BookingUser;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingCommentRepository extends JpaRepository<BookingComment, UUID> {

    @EntityGraph(attributePaths = {"teacher"})
    List<BookingComment> findAllByBookingUserOrderByCreatedAtAsc(BookingUser bookingUser);

    @EntityGraph(attributePaths = {"teacher"})
    List<BookingComment> findAllByBookingUserInOrderByCreatedAtAsc(Collection<BookingUser> bookingUsers);
}
