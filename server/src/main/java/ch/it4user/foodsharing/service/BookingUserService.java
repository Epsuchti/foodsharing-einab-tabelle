package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import java.util.UUID;
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
    public BookingUser getOrCreate(String email, String name, String foodsharingId, String phoneNumber, LanguageCode language) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedName = name.trim();
        String normalizedFoodsharingId = foodsharingId.trim();
        String normalizedPhoneNumber = phoneNumber.trim();

        bookingUserRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId)
                .filter(user -> !user.isActive())
                .ifPresent(user -> {
                    throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.BOOKING_USER_DISABLED);
                });

        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizedFoodsharingId)
                .map(existing -> {
                    existing.setEmail(normalizedEmail);
                    existing.setName(normalizedName);
                    existing.setPhoneNumber(normalizedPhoneNumber);
                    existing.setPreferredLanguage(language);
                    return existing;
                })
                .orElseGet(() -> {
                    BookingUser bookingUser = new BookingUser();
                    bookingUser.setEmail(normalizedEmail);
                    bookingUser.setName(normalizedName);
                    bookingUser.setFoodsharingId(normalizedFoodsharingId);
                    bookingUser.setPhoneNumber(normalizedPhoneNumber);
                    bookingUser.setActive(true);
                    bookingUser.setPreferredLanguage(language);
                    return bookingUserRepository.save(bookingUser);
                });
    }

    public BookingUser getByFoodsharingId(String foodsharingId) {
        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(foodsharingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    @Transactional
    public BookingUser disable(UUID bookingUserId) {
        BookingUser bookingUser = bookingUserRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
        bookingUser.setActive(false);
        return bookingUserRepository.save(bookingUser);
    }
}
