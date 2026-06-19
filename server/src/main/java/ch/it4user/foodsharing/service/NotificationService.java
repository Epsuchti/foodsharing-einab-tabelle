package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import ch.it4user.foodsharing.repository.NotificationSubscriptionRepository;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationSubscriptionRepository notificationSubscriptionRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final AppProperties appProperties;

    public NotificationService(NotificationSubscriptionRepository notificationSubscriptionRepository,
                               TokenService tokenService,
                               EmailService emailService,
                               EmailTemplateService emailTemplateService,
                               AppProperties appProperties) {
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.appProperties = appProperties;
    }

    @Transactional
    public NotificationSubscription subscribe(String email) {
        return notificationSubscriptionRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .map(existing -> {
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> {
                    NotificationSubscription subscription = new NotificationSubscription();
                    subscription.setEmail(email.trim().toLowerCase());
                    subscription.setActive(true);
                    subscription.setUnsubscribeToken(tokenService.generateToken());
                    return notificationSubscriptionRepository.save(subscription);
                });
    }

    @Transactional
    public NotificationSubscription unsubscribe(String token) {
        NotificationSubscription subscription = notificationSubscriptionRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid unsubscribe token"));
        subscription.setActive(false);
        return subscription;
    }

    public void notifyNewEinAb(EinAb einAb) {
        List<NotificationSubscription> recipients = notificationSubscriptionRepository.findAllByActiveTrueOrderByCreatedAtAsc();
        for (NotificationSubscription recipient : recipients) {
            String unsubscribeUrl = appProperties.getFrontend().getBaseUrl()
                    + "/unsubscribe?token=" + recipient.getUnsubscribeToken();
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Teacher", einAb.getTeacher().getName());
            details.put("Category", einAb.getCategory().name());
            details.put("Start", String.valueOf(einAb.getStartDateTime()));
            details.put("Fairteiler visit", einAb.isVisitFairteiler() ? "yes" : "no");
            String body = emailTemplateService.render(
                    "New EinAb slot",
                    emailTemplateService.paragraph("Hello,")
                            + emailTemplateService.paragraph("A new EinAb slot is available.")
                            + emailTemplateService.detailsTable(details)
                            + emailTemplateService.button("Unsubscribe", unsubscribeUrl)
            );
            emailService.send(recipient.getEmail(), "New foodsharing EinAb slot", body);
        }
    }
}
