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
    private final FoodsharingClient foodsharingClient;

    public BookingUserService(BookingUserRepository bookingUserRepository, FoodsharingClient foodsharingClient) {
        this.bookingUserRepository = bookingUserRepository;
        this.foodsharingClient = foodsharingClient;
    }

    @Transactional
    public BookingUser getOrCreate(String foodsharingId, LanguageCode language) {
        String normalizedFoodsharingId = normalizeFoodsharingId(foodsharingId);
        FoodsharingUserInfo foodsharingUser = foodsharingClient.getUser(normalizedFoodsharingId);
        if (foodsharingUser.sleeping()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.BOOKING_USER_DISABLED);
        }
        return bookingUserRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId)
                .map(existing -> {
                    existing.setName(foodsharingUser.name());
                    existing.setActive(true);
                    existing.setPreferredLanguage(language);
                    return existing;
                })
                .orElseGet(() -> {
                    BookingUser bookingUser = new BookingUser();
                    bookingUser.setName(foodsharingUser.name());
                    bookingUser.setFoodsharingId(normalizedFoodsharingId);
                    bookingUser.setActive(true);
                    bookingUser.setPreferredLanguage(language);
                    return bookingUserRepository.save(bookingUser);
                });
    }

    public BookingUser getByFoodsharingId(String foodsharingId) {
        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizeFoodsharingId(foodsharingId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    @Transactional
    public BookingUser disable(UUID bookingUserId) {
        BookingUser bookingUser = bookingUserRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
        bookingUser.setActive(false);
        return bookingUser;
    }

    private String normalizeFoodsharingId(String foodsharingId) {
        String normalized = foodsharingId == null ? "" : foodsharingId.trim();
        if (!normalized.matches("\\d+")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
        return normalized;
    }
}
