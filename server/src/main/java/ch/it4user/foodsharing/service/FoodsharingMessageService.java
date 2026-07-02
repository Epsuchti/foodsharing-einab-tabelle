package ch.it4user.foodsharing.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class FoodsharingMessageService {
    private final FoodsharingClient foodsharingClient;

    public FoodsharingMessageService(FoodsharingClient foodsharingClient) {
        this.foodsharingClient = foodsharingClient;
    }

    public void send(String recipientFoodsharingId, String subject, String htmlBody) {
        String plainBody = HtmlUtils.htmlUnescape(htmlBody.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim());
        foodsharingClient.sendMessage(recipientFoodsharingId, subject + "\n\n" + plainBody);
    }
}
