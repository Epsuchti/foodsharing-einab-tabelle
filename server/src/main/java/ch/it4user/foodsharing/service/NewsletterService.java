package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.NotificationSubscription;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import ch.it4user.foodsharing.repository.EinAbRepository;
import ch.it4user.foodsharing.repository.NotificationSubscriptionRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsletterService {

    private final NotificationSubscriptionRepository notificationSubscriptionRepository;
    private final EinAbRepository einAbRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final NewsletterTemplateService newsletterTemplateService;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher eventPublisher;

    public NewsletterService(NotificationSubscriptionRepository notificationSubscriptionRepository,
                             EinAbRepository einAbRepository,
                             TokenService tokenService,
                             EmailService emailService,
                             NewsletterTemplateService newsletterTemplateService,
                             AppProperties appProperties,
                             ApplicationEventPublisher eventPublisher) {
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
        this.einAbRepository = einAbRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.newsletterTemplateService = newsletterTemplateService;
        this.appProperties = appProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public NotificationSubscription subscribe(String email, LanguageCode language) {
        String normalizedEmail = normalizeEmail(email);
        LanguageCode normalizedLanguage = language == null ? LanguageCode.DE : language;
        return notificationSubscriptionRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> {
                    existing.setActive(true);
                    existing.setLanguage(normalizedLanguage);
                    if (existing.getUnsubscribeToken() == null || existing.getUnsubscribeToken().isBlank()) {
                        existing.setUnsubscribeToken(tokenService.generateToken());
                    }
                    eventPublisher.publishEvent(new NotificationSubscriptionCreatedEvent(existing.getId()));
                    return existing;
                })
                .orElseGet(() -> {
                    NotificationSubscription subscription = new NotificationSubscription();
                    subscription.setEmail(normalizedEmail);
                    subscription.setLanguage(normalizedLanguage);
                    subscription.setActive(true);
                    subscription.setUnsubscribeToken(tokenService.generateToken());
                    NotificationSubscription saved = notificationSubscriptionRepository.save(subscription);
                    eventPublisher.publishEvent(new NotificationSubscriptionCreatedEvent(saved.getId()));
                    return saved;
                });
    }

    @Transactional
    public NotificationSubscription unsubscribe(String token) {
        NotificationSubscription subscription = notificationSubscriptionRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_UNSUBSCRIBE_TOKEN));
        subscription.setActive(false);
        return subscription;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEinAbCreated(EinAbCreatedEvent event) {
        EinAb einAb = einAbRepository.findWithTeacherById(event.einAbId()).orElse(null);
        if (einAb == null) {
            return;
        }

        for (NotificationSubscription recipient : notificationSubscriptionRepository.findAllByActiveTrueOrderByCreatedAtAsc()) {
            String unsubscribeUrl = appProperties.getFrontend().getBaseUrl() + "/unsubscribe?token=" + recipient.getUnsubscribeToken();
            LanguageCode language = recipient.getLanguage() == null ? LanguageCode.DE : recipient.getLanguage();
            emailService.send(
                    recipient.getEmail(),
                    newsletterTemplateService.newEinAbSubject(language),
                    newsletterTemplateService.newEinAbBodyText(language, einAb, unsubscribeUrl),
                    newsletterTemplateService.newEinAbBodyHtml(language, einAb, unsubscribeUrl));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationSubscriptionCreated(NotificationSubscriptionCreatedEvent event) {
        NotificationSubscription subscription = notificationSubscriptionRepository.findById(event.subscriptionId()).orElse(null);
        if (subscription == null || !subscription.isActive()) {
            return;
        }
        LanguageCode language = subscription.getLanguage() == null ? LanguageCode.DE : subscription.getLanguage();
        String unsubscribeUrl = appProperties.getFrontend().getBaseUrl() + "/unsubscribe?token=" + subscription.getUnsubscribeToken();
        emailService.send(
                subscription.getEmail(),
                newsletterTemplateService.subscriptionConfirmationSubject(language),
                newsletterTemplateService.subscriptionConfirmationBodyText(language, unsubscribeUrl),
                newsletterTemplateService.subscriptionConfirmationBodyHtml(language, unsubscribeUrl));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, java.util.List.of("Email is required."));
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        try {
            new InternetAddress(normalizedEmail).validate();
        } catch (AddressException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, java.util.List.of("Email is invalid."));
        }
        return normalizedEmail;
    }
}
