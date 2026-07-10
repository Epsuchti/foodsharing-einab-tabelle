package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingUserService {

    private final UserRepository bookingUserRepository;
    private final FoodsharingClient foodsharingClient;

    public BookingUserService(UserRepository bookingUserRepository, FoodsharingClient foodsharingClient) {
        this.bookingUserRepository = bookingUserRepository;
        this.foodsharingClient = foodsharingClient;
    }

    @Transactional
    public User getOrCreate(String foodsharingId, LanguageCode language) {
        String normalizedFoodsharingId = normalizeFoodsharingId(foodsharingId);
        FoodsharingUserInfo foodsharingUser = foodsharingClient.getUser(normalizedFoodsharingId);
        if (foodsharingUser.sleeping()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.BOOKING_USER_DISABLED);
        }
        return bookingUserRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId)
                .map(existing -> {
                    existing.setName(foodsharingUser.name());
                    if (foodsharingUser.phoneNumber() != null) {
                        existing.setPhoneNumber(foodsharingUser.phoneNumber());
                    }
                    existing.setActive(true);
                    existing.setPreferredLanguage(language);
                    return existing;
                })
                .orElseGet(() -> {
                    User bookingUser = new User();
                    bookingUser.setName(foodsharingUser.name());
                    bookingUser.setPhoneNumber(foodsharingUser.phoneNumber());
                    bookingUser.setFoodsharingId(normalizedFoodsharingId);
                    bookingUser.setActive(true);
                    bookingUser.setPreferredLanguage(language);
                    return bookingUserRepository.save(bookingUser);
                });
    }

    public User getByFoodsharingId(String foodsharingId) {
        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizeFoodsharingId(foodsharingId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    @Transactional
    public User disable(UUID bookingUserId) {
        User bookingUser = bookingUserRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
        bookingUser.setActive(false);
        return bookingUser;
    }

    @Transactional
    public User enable(UUID bookingUserId) {
        User bookingUser = bookingUserRepository.findById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
        bookingUser.setActive(true);
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
