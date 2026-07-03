package ch.it4user.foodsharing.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    public void send(String recipient, String subject, String body) {
        if (javaMailSender == null) {
            log.info("Email to {} [{}]:\n{}", recipient, subject, body);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            javaMailSender.send(message);
        } catch (Exception ex) {
            log.warn("SMTP unavailable, logging email to console instead: {}", ex.getMessage());
            log.info("Email to {} [{}]:\n{}", recipient, subject, body);
        }
    }

    public void send(String recipient, String subject, String plainTextBody, String htmlBody) {
        if (javaMailSender == null) {
            log.info("Email to {} [{}]:\n{}", recipient, subject, plainTextBody);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(plainTextBody, htmlBody);
            javaMailSender.send(message);
        } catch (Exception ex) {
            log.warn("SMTP unavailable, logging email to console instead: {}", ex.getMessage());
            log.info("Email to {} [{}]:\n{}", recipient, subject, plainTextBody);
        }
    }
}
