package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RoleResolutionService {

    private final TeacherRepository teacherRepository;
    private final BookingUserRepository bookingUserRepository;

    public RoleResolutionService(TeacherRepository teacherRepository,
                                 BookingUserRepository bookingUserRepository) {
        this.teacherRepository = teacherRepository;
        this.bookingUserRepository = bookingUserRepository;
    }

    public Set<UserRole> resolveRoles(String foodsharingId) {
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        String normalizedFoodsharingId = foodsharingId.trim();
        if (bookingUserRepository.existsByFoodsharingIdIgnoreCaseAndActiveTrue(normalizedFoodsharingId)) {
            roles.add(UserRole.USER);
        }
        teacherRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId).ifPresent(teacher -> {
            roles.add(UserRole.TEACHER);
            if (teacher.isAdmin()) {
                roles.add(UserRole.ADMIN);
            }
        });
        return roles;
    }
}
