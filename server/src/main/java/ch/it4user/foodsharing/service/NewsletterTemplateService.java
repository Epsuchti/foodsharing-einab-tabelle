package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class NewsletterTemplateService {

    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter SWISS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final MessageSource messageSource;

    public NewsletterTemplateService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String newEinAbSubject(LanguageCode language) {
        return message(language, "message.newsletter.new-einab.subject");
    }

    public String subscriptionConfirmationSubject(LanguageCode language) {
        return message(language, "message.newsletter.subscription.subject");
    }

    public String subscriptionConfirmationBodyText(LanguageCode language, String unsubscribeUrl) {
        LanguageCode resolved = language == null ? LanguageCode.DE : language;
        return String.join("\n",
                greeting(resolved),
                "",
                subscriptionIntro(resolved),
                "",
                subscriptionOutro(resolved),
                "",
                field(resolved, "unsubscribe") + ": " + unsubscribeUrl);
    }

    public String subscriptionConfirmationBodyHtml(LanguageCode language, String unsubscribeUrl) {
        LanguageCode resolved = language == null ? LanguageCode.DE : language;
        String subject = HtmlUtils.htmlEscape(subscriptionConfirmationSubject(resolved));
        return """
                <html>
                <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                    <div style="background:#ffffff;border:1px solid #dbe3ee;border-radius:8px;overflow:hidden;">
                      <div style="padding:20px 24px;background:#0f766e;color:#ffffff;">
                        <div style="font-size:14px;letter-spacing:0.02em;text-transform:uppercase;opacity:0.9;">%s</div>
                        <div style="font-size:24px;font-weight:700;margin-top:8px;">%s</div>
                      </div>
                      <div style="padding:24px;">
                        <p style="margin:0 0 12px 0;line-height:1.5;">%s</p>
                        <p style="margin:0 0 16px 0;line-height:1.5;">%s</p>
                        <p style="margin:20px 0 0 0;line-height:1.5;">%s</p>
                        <p style="margin:24px 0 0 0;">
                          <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:700;">%s</a>
                        </p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(header(resolved)),
                subject,
                HtmlUtils.htmlEscape(greeting(resolved)),
                HtmlUtils.htmlEscape(subscriptionIntro(resolved)),
                HtmlUtils.htmlEscape(subscriptionOutro(resolved)),
                HtmlUtils.htmlEscape(unsubscribeUrl),
                HtmlUtils.htmlEscape(unsubscribeLabel(resolved))
        );
    }

    public String newEinAbBodyText(LanguageCode language, EinAb einAb, String unsubscribeUrl) {
        LanguageCode resolved = language == null ? LanguageCode.DE : language;
        return String.join("\n",
                greeting(resolved),
                "",
                intro(resolved),
                "",
                field(resolved, "category") + ": " + categoryLabel(resolved, einAb),
                field(resolved, "start") + ": " + swissDateTime(einAb.getStartDateTime()),
                field(resolved, "location") + ": " + valueOrDash(einAb.getPublicLocation()),
                field(resolved, "fairteiler") + ": " + yesNo(resolved, einAb.isVisitFairteiler()),
                "",
                field(resolved, "unsubscribe") + ": " + unsubscribeUrl);
    }

    public String newEinAbBodyHtml(LanguageCode language, EinAb einAb, String unsubscribeUrl) {
        LanguageCode resolved = language == null ? LanguageCode.DE : language;
        String subject = HtmlUtils.htmlEscape(newEinAbSubject(resolved));
        return """
                <html>
                <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                    <div style="background:#ffffff;border:1px solid #dbe3ee;border-radius:8px;overflow:hidden;">
                      <div style="padding:20px 24px;background:#0f766e;color:#ffffff;">
                        <div style="font-size:14px;letter-spacing:0.02em;text-transform:uppercase;opacity:0.9;">%s</div>
                        <div style="font-size:24px;font-weight:700;margin-top:8px;">%s</div>
                      </div>
                      <div style="padding:24px;">
                        <p style="margin:0 0 12px 0;line-height:1.5;">%s</p>
                        <p style="margin:0 0 16px 0;line-height:1.5;">%s</p>
                        %s
                        <p style="margin:20px 0 0 0;line-height:1.5;">%s</p>
                        <p style="margin:24px 0 0 0;">
                          <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:700;">%s</a>
                        </p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(header(resolved)),
                subject,
                HtmlUtils.htmlEscape(greeting(resolved)),
                HtmlUtils.htmlEscape(intro(resolved)),
                renderFieldTable(resolved, einAb),
                HtmlUtils.htmlEscape(outro(resolved)),
                HtmlUtils.htmlEscape(unsubscribeUrl),
                HtmlUtils.htmlEscape(unsubscribeLabel(resolved))
        );
    }

    public String swissDateTime(java.time.Instant value) {
        if (value == null) {
            return "";
        }
        return SWISS_DATE_TIME.format(value.atZone(SWISS_ZONE));
    }

    private String renderFieldTable(LanguageCode language, EinAb einAb) {
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="width:100%%;border-collapse:collapse;margin:16px 0 0 0;">
                  %s
                  %s
                  %s
                  %s
                </table>
                """.formatted(
                fieldRow(language, "category", categoryLabel(language, einAb)),
                fieldRow(language, "start", swissDateTime(einAb.getStartDateTime())),
                fieldRow(language, "location", valueOrDash(einAb.getPublicLocation())),
                fieldRow(language, "fairteiler", yesNo(language, einAb.isVisitFairteiler()))
        );
    }

    private String fieldRow(LanguageCode language, String key, String value) {
        return """
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px;vertical-align:top;width:160px;">%s</td>
                  <td style="padding:8px 0;font-size:14px;vertical-align:top;">%s</td>
                </tr>
                """.formatted(HtmlUtils.htmlEscape(field(language, key)), HtmlUtils.htmlEscape(value));
    }

    private String intro(LanguageCode language) { return message(language, "message.newsletter.new-einab.intro"); }

    private String subscriptionIntro(LanguageCode language) { return message(language, "message.newsletter.subscription.intro"); }

    private String subscriptionOutro(LanguageCode language) { return message(language, "message.newsletter.subscription.outro"); }

    private String outro(LanguageCode language) { return message(language, "message.newsletter.new-einab.outro"); }

    private String header(LanguageCode language) { return message(language, "message.newsletter.header"); }

    private String greeting(LanguageCode language) { return message(language, "message.greeting"); }

    private String unsubscribeLabel(LanguageCode language) { return message(language, "message.newsletter.unsubscribe"); }

    private String field(LanguageCode language, String key) { return message(language, "unsubscribe".equals(key) ? "message.newsletter.unsubscribe" : "message.details." + key); }

    private String yesNo(LanguageCode language, boolean value) { return message(language, value ? "message.yes" : "message.no"); }

    private String categoryLabel(LanguageCode language, EinAb einAb) { return message(language, "message.category." + einAb.getCategory().name().toLowerCase(Locale.ROOT)); }

    private String message(LanguageCode language, String key, Object... arguments) {
        LanguageCode resolved = language == null ? LanguageCode.DE : language;
        return messageSource.getMessage(key, arguments, Locale.forLanguageTag(resolved.getCode()));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
