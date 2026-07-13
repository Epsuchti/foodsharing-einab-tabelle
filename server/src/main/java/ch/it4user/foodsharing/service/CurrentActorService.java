package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.User;
import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentActorService {

    private final UserRepository userRepository;

    public CurrentActorService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    public User requireTeacher() {
        return userRepository.findByFoodsharingIdIgnoreCase(requireFoodsharingId())
                .filter(user -> hasPermission(user, UserPermission.CAN_GIVE_EIN_ABS))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.TEACHER_ACCOUNT_REQUIRED));
    }

    public User requireAdmin() {
        return requirePermission(UserPermission.CAN_MANAGE_USERS);
    }

    public User requireAutomationUser() {
        return userRepository.findByFoodsharingIdIgnoreCase(requireFoodsharingId())
                .filter(user -> hasPermission(user, UserPermission.CAN_USE_AUTOMATIONS)
                        || hasPermission(user, UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED));
    }

    public User requireAutomationSlotApprovalUser() {
        return requirePermission(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
    }

    public User requirePermission(UserPermission permission) {
        return userRepository.findByFoodsharingIdIgnoreCase(requireFoodsharingId())
                .filter(user -> hasPermission(user, permission))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED));
    }

    public boolean hasPermission(UserPermission permission) {
        return userRepository.findByFoodsharingIdIgnoreCase(requireFoodsharingId())
                .map(user -> hasPermission(user, permission))
                .orElse(false);
    }

    private boolean hasPermission(User user, UserPermission permission) {
        return switch (permission) {
            case CAN_GIVE_EIN_ABS -> user.isCanGiveEinAbs();
            case CAN_MANAGE_USERS -> user.isCanManageUsers();
            case CAN_USE_AUTOMATIONS -> user.isCanUseAutomations();
            case CAN_SEE_USER_PICKUP_COUNT_GROUPING -> user.isCanSeeUserPickupCountGrouping();
            case CAN_USE_AUTOMATION_SLOT_APPROVAL -> user.isCanUseAutomationSlotApproval();
            case CAN_USE_AUTOMATION_REQUEST_APPROVAL -> user.isCanUseAutomationRequestApproval();
            case CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING -> user.isCanUseAutomationOpenSlotAdvertising();
            case CAN_SEE_ALL_AUTOMATION_DECISIONS -> user.isCanSeeAllAutomationDecisions();
        };
    }

    public boolean canManageUsers() {
        return hasPermission(UserPermission.CAN_MANAGE_USERS);
    }
}
