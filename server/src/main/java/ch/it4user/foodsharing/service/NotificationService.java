package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
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
    public NotificationSubscription subscribe(String email, LanguageCode language) {
        return notificationSubscriptionRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .map(existing -> {
                    existing.setActive(true);
                    existing.setPreferredLanguage(language);
                    return existing;
                })
                .orElseGet(() -> {
                    NotificationSubscription subscription = new NotificationSubscription();
                    subscription.setEmail(email.trim().toLowerCase());
                    subscription.setActive(true);
                    subscription.setUnsubscribeToken(tokenService.generateToken());
                    subscription.setPreferredLanguage(language);
                    return notificationSubscriptionRepository.save(subscription);
                });
    }

    @Transactional
    public NotificationSubscription unsubscribe(String token) {
        NotificationSubscription subscription = notificationSubscriptionRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_UNSUBSCRIBE_TOKEN));
        subscription.setActive(false);
        return subscription;
    }

    public void notifyNewEinAb(EinAb einAb) {
        List<NotificationSubscription> recipients = notificationSubscriptionRepository.findAllByActiveTrueOrderByCreatedAtAsc();
        for (NotificationSubscription recipient : recipients) {
            String unsubscribeUrl = appProperties.getFrontend().getBaseUrl()
                    + "/unsubscribe?token=" + recipient.getUnsubscribeToken();
            emailService.send(
                    recipient.getEmail(),
                    emailTemplateService.notificationSubject(recipient.getPreferredLanguage()),
                    emailTemplateService.notificationBody(recipient.getPreferredLanguage(), einAb, unsubscribeUrl));
        }
    }

    public void notifyTeacherCancelledBooking(Slot slot) {
        if (slot.getBookingUser() == null) {
            return;
        }
        String manageUrl = appProperties.getFrontend().getBaseUrl() + "/my-bookings";
        emailService.send(
                slot.getBookingUser().getEmail(),
                emailTemplateService.teacherCancellationSubject(slot.getBookingUser().getPreferredLanguage()),
                emailTemplateService.teacherCancellationBody(slot.getBookingUser().getPreferredLanguage(), slot, manageUrl));
    }
}
