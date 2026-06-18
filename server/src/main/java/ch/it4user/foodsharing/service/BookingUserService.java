package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingUserService {

    private final BookingUserRepository bookingUserRepository;

    public BookingUserService(BookingUserRepository bookingUserRepository) {
        this.bookingUserRepository = bookingUserRepository;
    }

    @Transactional
    public BookingUser getOrCreate(String email, String foodsharingId, String phoneNumber) {
        return bookingUserRepository.findByEmailIgnoreCaseAndFoodsharingIdIgnoreCase(email, foodsharingId)
                .map(existing -> {
                    existing.setPhoneNumber(phoneNumber.trim());
                    return existing;
                })
                .orElseGet(() -> {
                    BookingUser bookingUser = new BookingUser();
                    bookingUser.setEmail(email.trim().toLowerCase());
                    bookingUser.setFoodsharingId(foodsharingId.trim());
                    bookingUser.setPhoneNumber(phoneNumber.trim());
                    return bookingUserRepository.save(bookingUser);
                });
    }

    public List<BookingUser> findByEmail(String email) {
        return bookingUserRepository.findAllByEmailIgnoreCase(email);
    }

    public List<BookingUser> findAll() {
        return bookingUserRepository.findAll();
    }
}
