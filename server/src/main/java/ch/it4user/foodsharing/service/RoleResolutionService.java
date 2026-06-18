package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.repository.BookingUserRepository;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RoleResolutionService {

    private final AppProperties appProperties;
    private final TeacherRepository teacherRepository;
    private final BookingUserRepository bookingUserRepository;

    public RoleResolutionService(AppProperties appProperties,
                                 TeacherRepository teacherRepository,
                                 BookingUserRepository bookingUserRepository) {
        this.appProperties = appProperties;
        this.teacherRepository = teacherRepository;
        this.bookingUserRepository = bookingUserRepository;
    }

    public Set<UserRole> resolveRoles(String email) {
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        String normalizedEmail = email.trim().toLowerCase();
        if (bookingUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            roles.add(UserRole.USER);
        }
        if (teacherRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            roles.add(UserRole.TEACHER);
        }
        if (appProperties.getAuth().getAdminEmails().stream()
                .map(String::toLowerCase)
                .anyMatch(normalizedEmail::equals)) {
            roles.add(UserRole.ADMIN);
        }
        return roles;
    }
}
