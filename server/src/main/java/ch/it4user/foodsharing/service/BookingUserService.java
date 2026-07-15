package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.repository.BezirkRepository;
import ch.it4user.foodsharing.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingUserService {

    private final UserRepository bookingUserRepository;
    private final BezirkRepository bezirkRepository;
    private final FoodsharingClient foodsharingClient;

    public BookingUserService(UserRepository bookingUserRepository,
                              BezirkRepository bezirkRepository,
                              FoodsharingClient foodsharingClient) {
        this.bookingUserRepository = bookingUserRepository;
        this.bezirkRepository = bezirkRepository;
        this.foodsharingClient = foodsharingClient;
    }

    @Transactional
    public User getOrCreate(String foodsharingId, LanguageCode language) {
        String normalizedFoodsharingId = normalizeFoodsharingId(foodsharingId);
        FoodsharingUserInfo foodsharingUser = foodsharingClient.getUser(normalizedFoodsharingId);
        User bookingUser = bookingUserRepository.findByFoodsharingIdIgnoreCaseForUpdate(normalizedFoodsharingId)
                .orElseGet(() -> createUserWithIdentityLock(normalizedFoodsharingId));
        bookingUser.setName(foodsharingUser.name());
        if (foodsharingUser.phoneNumber() != null) {
            bookingUser.setPhoneNumber(foodsharingUser.phoneNumber());
        }
        bookingUser.setPreferredLanguage(language);
        return bookingUser;
    }

    public void assignToBezirk(User bookingUser, Bezirk bezirk) {
        if (bookingUser.getBezirk() == null) {
            bookingUser.setBezirk(bezirk);
            return;
        }
        if (!bookingUser.getBezirk().getId().equals(bezirk.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.USER_BEZIRK_MISMATCH);
        }
    }

    public User getByFoodsharingId(String foodsharingId) {
        return bookingUserRepository.findByFoodsharingIdIgnoreCaseAndActiveTrue(normalizeFoodsharingId(foodsharingId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
    }

    @Transactional
    public User disable(UUID bookingUserId) {
        User bookingUser = bookingUserRepository.findWithBezirkById(bookingUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BOOKING_USER_NOT_FOUND));
        bookingUser.setActive(false);
        return bookingUser;
    }

    @Transactional
    public User enable(UUID bookingUserId) {
        User bookingUser = bookingUserRepository.findWithBezirkById(bookingUserId)
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

    private User createUserWithIdentityLock(String normalizedFoodsharingId) {
        bezirkRepository.findFirstByOrderBySortOrderAscIdAsc()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BEZIRK_NOT_FOUND));
        return bookingUserRepository.findByFoodsharingIdIgnoreCaseForUpdate(normalizedFoodsharingId)
                .orElseGet(() -> {
                    User bookingUser = new User();
                    bookingUser.setFoodsharingId(normalizedFoodsharingId);
                    bookingUser.setName(normalizedFoodsharingId);
                    bookingUser.setActive(true);
                    return bookingUserRepository.saveAndFlush(bookingUser);
                });
    }
}
