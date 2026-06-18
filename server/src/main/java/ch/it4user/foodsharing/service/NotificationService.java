package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import ch.it4user.foodsharing.repository.NotificationSubscriptionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationSubscriptionRepository notificationSubscriptionRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public NotificationService(NotificationSubscriptionRepository notificationSubscriptionRepository,
                               TokenService tokenService,
                               EmailService emailService,
                               AppProperties appProperties) {
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
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
            String body = """
                    <html>
                      <body style="font-family: Arial, sans-serif; color: #1f2937;">
                        <p>Hello,</p>
                        <p>A new EinAb slot is available.</p>
                        <ul>
                          <li><strong>Teacher:</strong> %s</li>
                          <li><strong>Category:</strong> %s</li>
                          <li><strong>Start:</strong> %s</li>
                          <li><strong>Fairteiler visit:</strong> %s</li>
                        </ul>
                        <p><a href="%s">Unsubscribe</a></p>
                      </body>
                    </html>
                    """.formatted(
                    einAb.getTeacher().getName(),
                    einAb.getCategory().name(),
                    einAb.getStartDateTime(),
                    einAb.isVisitFairteiler() ? "yes" : "no",
                    unsubscribeUrl
            );
            emailService.send(recipient.getEmail(), "New foodsharing EinAb slot", body);
        }
    }
}
