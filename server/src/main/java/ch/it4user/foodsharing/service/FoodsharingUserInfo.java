package ch.it4user.foodsharing.service;

import java.time.Instant;

public record FoodsharingUserInfo(String foodsharingId, String name, String phoneNumber, boolean sleeping, Instant verificationDate) {
}
