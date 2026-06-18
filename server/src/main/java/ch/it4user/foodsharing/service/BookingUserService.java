package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        bookingUserRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId)
                .filter(user -> !user.isActive())
                .ifPresent(user -> {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Booking user is disabled");
                });

        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizedFoodsharingId)
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
                    bookingUser.setActive(true);
                    return bookingUserRepository.save(bookingUser);
                });
    }

    public List<BookingUser> findByEmail(String email) {
        return bookingUserRepository.findAllByEmailIgnoreCaseAndActiveTrue(email);
    }

    public BookingUser getByEmail(String email) {
        return bookingUserRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking user not found"));
    }

    public List<BookingUser> findAll() {
        return bookingUserRepository.findAllByActiveTrueOrderByCreatedAtDesc();
    }

    public Page<BookingUser> findAll(Pageable pageable) {
        return bookingUserRepository.findAllActive(pageable);
    }

    @Transactional
    public BookingUser disable(UUID bookingUserId) {
        BookingUser bookingUser = bookingUserRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking user not found"));
        bookingUser.setActive(false);
        return bookingUserRepository.save(bookingUser);
    }

    private void assertEmailIsAvailable(String email, java.util.UUID currentBookingUserId) {
        boolean inUseByAnotherBookingUser = bookingUserRepository.findAllByEmailIgnoreCaseAndActiveTrue(email).stream()
                .anyMatch(user -> currentBookingUserId == null || !user.getId().equals(currentBookingUserId));
        if (inUseByAnotherBookingUser) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists for another booking user");
        }
    }
}
