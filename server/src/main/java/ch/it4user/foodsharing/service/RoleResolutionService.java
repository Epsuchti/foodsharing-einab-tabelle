package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.repository.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RoleResolutionService {

    private final UserRepository userRepository;

    public RoleResolutionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Set<UserRole> resolveRoles(String foodsharingId) {
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        String normalizedFoodsharingId = foodsharingId.trim();
        userRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId).ifPresent(user -> {
            if (user.isActive()) {
                roles.add(UserRole.USER);
            }
            if (user.isTeacher()) {
                roles.add(UserRole.TEACHER);
                if (user.isAdmin()) {
                    roles.add(UserRole.ADMIN);
                }
            }
        });
        return roles;
    }
}
