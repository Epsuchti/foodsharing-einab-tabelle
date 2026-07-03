package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class NewsletterTemplateService {

    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter SWISS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String newEinAbSubject(LanguageCode language) {
        return switch (language == null ? LanguageCode.DE : language) {
            case EN -> "New EinAb available";
            case GWS -> "Neui EinAb verfügbar";
            case DE -> "Neue EinAb verfügbar";
        };
    }

    public String subscriptionConfirmationSubject(LanguageCode language) {
        return switch (language == null ? LanguageCode.DE : language) {
            case EN -> "Your New Slots Newsletter subscription";
            case GWS -> "Dini Newsletter-Abonnemänt für neui Slots";
            case DE -> "Dein Newsletter für neue Slots";
        };
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

    private String intro(LanguageCode language) {
        return switch (language) {
            case EN -> "A new EinAb has been published:";
            case GWS -> "Es git es neus EinAb:";
            case DE -> "Es gibt einen neuen EinAb:";
        };
    }

    private String subscriptionIntro(LanguageCode language) {
        return switch (language) {
            case EN -> "Thanks for subscribing to the New Slots Newsletter.";
            case GWS -> "Danke fürs Abonnierä vom Newsletter für neui Slots.";
            case DE -> "Danke für dein Abonnement des Newsletters für neue Slots.";
        };
    }

    private String subscriptionOutro(LanguageCode language) {
        return switch (language) {
            case EN -> "You will now receive a message when new slots are published.";
            case GWS -> "Du bechunsch jetzt e Nachricht, wenn neui Slots veröffentlicht wärde.";
            case DE -> "Du erhältst jetzt eine Nachricht, wenn neue Slots veröffentlicht werden.";
        };
    }

    private String outro(LanguageCode language) {
        return switch (language) {
            case EN -> "Use the link below to unsubscribe from future slot notifications.";
            case GWS -> "Mit em Link chasch d Benachrichtigige für neui Slots abbestelle.";
            case DE -> "Mit dem Link kannst du dich von künftigen Slot-Benachrichtigungen abmelden.";
        };
    }

    private String header(LanguageCode language) {
        return switch (language) {
            case EN -> "New slots newsletter";
            case GWS -> "Newsletter für neui Slots";
            case DE -> "Newsletter für neue Slots";
        };
    }

    private String greeting(LanguageCode language) {
        return switch (language) {
            case EN -> "Hello,";
            case GWS -> "Hoi,";
            case DE -> "Hallo,";
        };
    }

    private String unsubscribeLabel(LanguageCode language) {
        return switch (language) {
            case EN -> "Unsubscribe";
            case GWS -> "Abmälde";
            case DE -> "Abmelden";
        };
    }

    private String field(LanguageCode language, String key) {
        return switch (key) {
            case "category" -> switch (language) {
                case EN -> "Category";
                case GWS -> "Kategorie";
                case DE -> "Kategorie";
            };
            case "start" -> switch (language) {
                case EN -> "Start";
                case GWS -> "Start";
                case DE -> "Start";
            };
            case "location" -> switch (language) {
                case EN -> "Location";
                case GWS -> "Ort";
                case DE -> "Ort";
            };
            case "fairteiler" -> switch (language) {
                case EN -> "Fairteiler visit";
                case GWS -> "Fairteiler-Bsuech";
                case DE -> "Fairteiler-Besuch";
            };
            case "unsubscribe" -> switch (language) {
                case EN -> "Unsubscribe";
                case GWS -> "Abmälde";
                case DE -> "Abmelden";
            };
            default -> key;
        };
    }

    private String yesNo(LanguageCode language, boolean value) {
        return switch (language) {
            case EN -> value ? "Yes" : "No";
            case GWS -> value ? "Jo" : "Nei";
            case DE -> value ? "Ja" : "Nein";
        };
    }

    private String categoryLabel(LanguageCode language, EinAb einAb) {
        return switch (einAb.getCategory()) {
            case SUPERMARKET -> switch (language) {
                case EN -> "Supermarket";
                case GWS -> "Supermärt";
                case DE -> "Supermarkt";
            };
            case TAKEOUT -> "Takeout";
            case MARKET -> switch (language) {
                case EN -> "Market";
                case GWS -> "Määrt";
                case DE -> "Markt";
            };
            case BAKERY -> switch (language) {
                case EN -> "Bakery";
                case GWS -> "Bäckerei";
                case DE -> "Bäckerei";
            };
            case RESTAURANT -> "Restaurant";
            case FAIRTEILER_CLEANING -> switch (language) {
                case EN -> "Fairteiler cleaning";
                case GWS -> "Fairteiler-Putz";
                case DE -> "Fairteiler-Reinigung";
            };
        };
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
