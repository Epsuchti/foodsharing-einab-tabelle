package ch.it4user.foodsharing.service;

import org.springframework.stereotype.Service;

@Service
public class FoodsharingMessageService {
    private final FoodsharingClient foodsharingClient;

    public FoodsharingMessageService(FoodsharingClient foodsharingClient) {
        this.foodsharingClient = foodsharingClient;
    }

    public void send(String recipientFoodsharingId, String subject, String body) {
        foodsharingClient.sendMessage(recipientFoodsharingId, subject + "\n\n" + normalize(body));
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim().replaceAll("\n{3,}", "\n\n");
    }
}
