package ch.it4user.foodsharing.service;

import java.time.Instant;
import java.util.List;

public final class FoodsharingPickupModels {
    private FoodsharingPickupModels() {}

    public record Store(long id, String name, boolean isManaging) {}
    public record StoreMember(long id, String name, Instant lastFetch, int fetchCount) {}
    public record Pickup(long storeId, Instant date, List<PickupUser> users) {}
    public record PickupUser(String id, String name, boolean confirmed) {}
    public record UserPickup(long storeId, String storeName, Instant date, boolean confirmed) {}
    public record Decision(boolean allowed, List<String> reasons, String userMessage) {}
    public record RunResult(int evaluated, int confirmed, int declined, int failed, boolean dryRun, List<String> messages) {}
}
