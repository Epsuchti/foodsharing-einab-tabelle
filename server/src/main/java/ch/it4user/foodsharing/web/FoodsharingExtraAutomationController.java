package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.domain.enumtype.UserPermission;
import ch.it4user.foodsharing.service.CurrentActorService;
import ch.it4user.foodsharing.service.FoodsharingPickupAutomationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/foodsharing")
public class FoodsharingExtraAutomationController {
    private final FoodsharingPickupAutomationService service;
    private final CurrentActorService currentActorService;

    public FoodsharingExtraAutomationController(FoodsharingPickupAutomationService service, CurrentActorService currentActorService) {
        this.service = service;
        this.currentActorService = currentActorService;
    }

    @GetMapping("/extra-automation-overview")
    public ResponseEntity<FoodsharingPickupAutomationService.ExtraAutomationOverviewView> getExtraAutomationOverview() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATIONS);
        return ResponseEntity.ok(service.extraAutomationOverview());
    }

    @GetMapping("/extra-automation-audit")
    public ResponseEntity<List<FoodsharingPickupAutomationService.ExtraAutomationAuditView>> getExtraAutomationAudit() {
        currentActorService.requirePermission(UserPermission.CAN_SEE_ALL_AUTOMATION_DECISIONS);
        return ResponseEntity.ok(service.extraAutomationAudit());
    }

    @GetMapping("/telegram/chats")
    public ResponseEntity<List<FoodsharingPickupAutomationService.TelegramChatView>> getTelegramChats() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        return ResponseEntity.ok(service.telegramChats());
    }

    @PostMapping("/request-automation/run")
    public ResponseEntity<FoodsharingPickupAutomationService.AutomationRunSummary> runRequestAutomationDryRun() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        return ResponseEntity.ok(service.runRequestAutomations(true));
    }

    @PostMapping("/open-slot-advertisements/run")
    public ResponseEntity<FoodsharingPickupAutomationService.AutomationRunSummary> runAdvertisementAutomationDryRun() {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        return ResponseEntity.ok(service.runAdvertisementAutomations(true));
    }

    @PostMapping("/telegram/test-message")
    public ResponseEntity<Void> sendTelegramTestMessage(@RequestBody TelegramTestMessageRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        service.sendTelegramTestMessage(request.chatId(), request.message());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/stores/{storeId}/request-automation")
    public ResponseEntity<FoodsharingPickupAutomationService.RequestAutomationView> saveRequestAutomation(
            @PathVariable long storeId,
            @RequestBody FoodsharingPickupAutomationService.RequestAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_REQUEST_APPROVAL);
        return ResponseEntity.ok(service.saveRequestAutomation(storeId, request));
    }

    @PutMapping("/stores/{storeId}/open-slot-advertisements/{advertNumber}")
    public ResponseEntity<FoodsharingPickupAutomationService.AdvertisementAutomationView> saveAdvertisementAutomation(
            @PathVariable long storeId,
            @PathVariable int advertNumber,
            @RequestBody FoodsharingPickupAutomationService.AdvertisementAutomationRequest request) {
        currentActorService.requirePermission(UserPermission.CAN_USE_AUTOMATION_OPEN_SLOT_ADVERTISING);
        return ResponseEntity.ok(service.saveAdvertisementAutomation(storeId, advertNumber, request));
    }

    public record TelegramTestMessageRequest(String chatId, String message) {}

    public record AdvertisementRequest(String storeName, boolean enabled, int triggerHoursBefore, boolean sendToStoreChat, boolean sendToTelegram, String telegramChatId, List<String> messages) {}
}
