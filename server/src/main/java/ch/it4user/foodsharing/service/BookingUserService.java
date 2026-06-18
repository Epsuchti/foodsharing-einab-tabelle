package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingUserService {

    private final BookingUserRepository bookingUserRepository;

    public BookingUserService(BookingUserRepository bookingUserRepository) {
        this.bookingUserRepository = bookingUserRepository;
    }

    @Transactional
    public BookingUser getOrCreate(String email, String name, String foodsharingId, String phoneNumber) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedName = name.trim();
        String normalizedFoodsharingId = foodsharingId.trim();
        String normalizedPhoneNumber = phoneNumber.trim();

        return bookingUserRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId)
                .map(existing -> {
                    assertEmailIsAvailable(normalizedEmail, existing.getId());
                    existing.setEmail(normalizedEmail);
                    existing.setName(normalizedName);
                    existing.setPhoneNumber(normalizedPhoneNumber);
                    return existing;
                })
                .orElseGet(() -> {
                    assertEmailIsAvailable(normalizedEmail, null);
                    BookingUser bookingUser = new BookingUser();
                    bookingUser.setEmail(normalizedEmail);
                    bookingUser.setName(normalizedName);
                    bookingUser.setFoodsharingId(normalizedFoodsharingId);
                    bookingUser.setPhoneNumber(normalizedPhoneNumber);
                    return bookingUserRepository.save(bookingUser);
                });
    }

    public List<BookingUser> findByEmail(String email) {
        return bookingUserRepository.findAllByEmailIgnoreCase(email);
    }

    public List<BookingUser> findAll() {
        return bookingUserRepository.findAll();
    }

    private void assertEmailIsAvailable(String email, java.util.UUID currentBookingUserId) {
        boolean inUseByAnotherBookingUser = bookingUserRepository.findAllByEmailIgnoreCase(email).stream()
                .anyMatch(user -> currentBookingUserId == null || !user.getId().equals(currentBookingUserId));
        if (inUseByAnotherBookingUser) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists for another booking user");
        }
    }
}
