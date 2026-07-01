package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.UserRole;
import ch.it4user.foodsharing.repository.TeacherRepository;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentActorService {

    private final TeacherRepository teacherRepository;

    public CurrentActorService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public String requireEmail() {
        return requirePrincipal();
    }

    public String requireFoodsharingId() {
        return requirePrincipal();
    }

    private String requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authentication.getName();
    }

    public Set<UserRole> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> UserRole.valueOf(authority.getAuthority().replace("ROLE_", "")))
                .collect(java.util.stream.Collectors.toSet());
    }

    public Teacher requireTeacher() {
        return teacherRepository.findByFoodsharingIdIgnoreCase(requireFoodsharingId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.TEACHER_ACCOUNT_REQUIRED));
    }

    public boolean isAdmin() {
        return currentRoles().contains(UserRole.ADMIN);
    }
}
