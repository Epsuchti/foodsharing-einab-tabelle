package ch.it4user.foodsharing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;
    private final AppProperties appProperties;

    public EmailService(ObjectProvider<JavaMailSender> javaMailSenderProvider, AppProperties appProperties) {
        this.javaMailSender = javaMailSenderProvider.getIfAvailable();
        this.appProperties = appProperties;
    }

    public void send(String recipient, String subject, String text) {
        if (javaMailSender == null) {
            log.info("Email to {} [{}]:\n{}", recipient, subject, text);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getMail().getFrom());
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("SMTP unavailable, logging email to console instead: {}", ex.getMessage());
            log.info("Email to {} [{}]:\n{}", recipient, subject, text);
        }
    }
}
