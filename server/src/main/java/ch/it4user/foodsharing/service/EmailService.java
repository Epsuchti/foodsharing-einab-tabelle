package ch.it4user.foodsharing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;
    private final AppProperties appProperties;

    public EmailService(ObjectProvider<JavaMailSender> javaMailSenderProvider, AppProperties appProperties) {
        this.javaMailSender = javaMailSenderProvider.getIfAvailable();
        this.appProperties = appProperties;
    }

    public void send(String recipient, String subject, String htmlBody) {
        if (javaMailSender == null) {
            log.info("Email to {} [{}]:\n{}", recipient, subject, htmlBody);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(message);
        } catch (MessagingException ex) {
            log.warn("SMTP message creation failed, logging email to console instead: {}", ex.getMessage());
            log.info("Email to {} [{}]:\n{}", recipient, subject, htmlBody);
        } catch (RuntimeException ex) {
            log.warn("SMTP unavailable, logging email to console instead: {}", ex.getMessage());
            log.info("Email to {} [{}]:\n{}", recipient, subject, htmlBody);
        }
    }
}
