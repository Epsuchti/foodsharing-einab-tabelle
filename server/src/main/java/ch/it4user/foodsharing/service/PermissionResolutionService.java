package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.repository.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PermissionResolutionService {

    private final UserRepository userRepository;

    public PermissionResolutionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Set<UserPermission> resolvePermissions(String foodsharingId) {
        EnumSet<UserPermission> permissions = EnumSet.noneOf(UserPermission.class);
        String normalizedFoodsharingId = foodsharingId.trim();
        userRepository.findByFoodsharingIdIgnoreCase(normalizedFoodsharingId).ifPresent(user -> {
            if (user.isCanGiveEinAbs()) permissions.add(UserPermission.CAN_GIVE_EIN_ABS);
            if (user.isCanManageUsers()) permissions.add(UserPermission.CAN_MANAGE_USERS);
            if (user.isCanUseAutomations()) permissions.add(UserPermission.CAN_USE_AUTOMATIONS);
            if (user.isCanSeeUserPickupCountGrouping()) permissions.add(UserPermission.CAN_SEE_USER_PICKUP_COUNT_GROUPING);
            if (user.isCanUseAutomationSlotApproval()) permissions.add(UserPermission.CAN_USE_AUTOMATION_SLOT_APPROVAL);
            if (user.isCanSeeAllAutomationDecisions()) permissions.add(UserPermission.CAN_SEE_ALL_AUTOMATION_DECISIONS);
        });
        return permissions;
    }
}
