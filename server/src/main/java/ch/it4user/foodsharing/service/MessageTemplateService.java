package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class MessageTemplateService {

    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter SWISS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String loginSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "Your foodsharing login link";
            case GWS -> "Din Foodsharing Login-Link";
            case DE -> "Dein Foodsharing Login-Link";
        };
    }

    public String loginBody(LanguageCode language, String loginLink) {
        return render(
                switch (language) {
                    case EN -> "Login link";
                    case GWS -> "Login-Link";
                    case DE -> "Login-Link";
                },
                paragraph(switch (language) {
                    case EN -> "Hello,";
                    case GWS -> "Hoi,";
                    case DE -> "Hallo,";
                })
                        + paragraph(switch (language) {
                            case EN -> "Open this link to sign in:";
                            case GWS -> "Mach dä Link uf zum Ilogge:";
                            case DE -> "Öffne diesen Link zum Einloggen:";
                        })
                        + button(switch (language) {
                            case EN -> "Sign in";
                            case GWS -> "Ilogge";
                            case DE -> "Einloggen";
                        }, loginLink)
                        + paragraph(switch (language) {
                            case EN -> "If you did not request this message, you can ignore it.";
                            case GWS -> "Wenn du die Nachricht nid aagforderet hesch, chasch si eifach ignoriere.";
                            case DE -> "Wenn du diese Nachricht nicht angefordert hast, kannst du sie ignorieren.";
                        }));
    }

    public String bookingConfirmationSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "Your foodsharing pickup details";
            case GWS -> "Dini Foodsharing Abholig";
            case DE -> "Deine Foodsharing-Abholung";
        };
    }

    public String bookingConfirmationBody(LanguageCode language, Slot slot, String manageUrl) {
        Map<String, String> details = bookingDetails(language, slot.getEinAb(), slot.getEinAb().getTeacher().getName(), slot.getEinAb().getTeacher().getPhoneNumber());
        return render(
                switch (language) {
                    case EN -> "Confirm booking";
                    case GWS -> "Buechig bestätige";
                    case DE -> "Buchung bestätigen";
                },
                paragraph(greeting(language, slot.getBookingUser().getName()))
                        + paragraph(switch (language) {
                            case EN -> "Please confirm this pickup within one hour.";
                            case GWS -> "Bitte bestätig die Abholig innerhalb vo einere Stund.";
                            case DE -> "Bitte bestätige diese Abholung innerhalb einer Stunde.";
                        })
                        + detailsTable(details)
                        + note(switch (language) {
                            case EN -> "You can log in later with the Foodsharing ID you used for this booking.";
                            case GWS -> "Du chasch di spöter mit dr Foodsharing-ID vo dere Buechig ilogge.";
                            case DE -> "Du kannst dich später mit der Foodsharing-ID dieser Buchung einloggen.";
                        })
                        + button(switch (language) {
                            case EN -> "Confirm pickup";
                            case GWS -> "Abholig bestätige";
                            case DE -> "Abholung bestätigen";
                        }, manageUrl));
    }

    public String teacherCancellationSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "Your pickup was cancelled";
            case GWS -> "Dini Abholig isch abgsagt worde";
            case DE -> "Deine Abholung wurde abgesagt";
        };
    }

    public String teacherCancellationBody(LanguageCode language, Slot slot, String manageUrl) {
        Map<String, String> details = bookingDetails(language, slot.getEinAb(), slot.getEinAb().getTeacher().getName(), slot.getEinAb().getTeacher().getPhoneNumber());
        return render(
                switch (language) {
                    case EN -> "Pickup cancelled";
                    case GWS -> "Abholig abgsagt";
                    case DE -> "Abholung abgesagt";
                },
                paragraph(greeting(language, slot.getBookingUser().getName()))
                        + paragraph(switch (language) {
                            case EN -> "The EinAb giver cancelled your booked pickup.";
                            case GWS -> "D EinAb-Geberin het dini buechti Abholig abgsagt.";
                            case DE -> "Die EinAb-Geberin hat deine gebuchte Abholung abgesagt.";
                        })
                        + detailsTable(details)
                        + button(switch (language) {
                            case EN -> "Open bookings";
                            case GWS -> "Buechige öffne";
                            case DE -> "Buchungen öffnen";
                        }, manageUrl));
    }

    public String render(String title, String bodyHtml) {
        return """
                <html>
                  <body style="margin:0;background:#f4f7f2;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                    <div style="max-width:720px;margin:0 auto;background:#ffffff;border:1px solid #d9e8d7;border-radius:16px;overflow:hidden;box-shadow:0 12px 30px rgba(15,23,42,0.08);">
                      <div style="background:#166534;color:#ffffff;padding:28px;">
                        <div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;opacity:.85;">Foodsharing EinAb</div>
                        <h1 style="margin:8px 0 0;font-size:28px;line-height:1.2;">%s</h1>
                      </div>
                      <div style="padding:28px;">%s</div>
                    </div>
                  </body>
                </html>
                """.formatted(escape(title), bodyHtml);
    }

    public String paragraph(String text) {
        return "<p style=\"margin:0 0 16px;line-height:1.6;\">" + escape(text) + "</p>";
    }

    public String paragraphHtml(String html) {
        return "<p style=\"margin:0 0 16px;line-height:1.6;\">" + html + "</p>";
    }

    public String button(String label, String url) {
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" style="margin:24px 0;">
                  <tr>
                    <td>
                      <a href="%s" style="display:inline-block;background:#16a34a;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:10px;font-weight:700;">%s</a>
                    </td>
                  </tr>
                </table>
                """.formatted(escape(url), escape(label));
    }

    public String link(String label, String url) {
        return """
                <a href="%s" style="color:#166534;text-decoration:underline;">%s</a>
                """.formatted(escape(url), escape(label));
    }

    public String detailsTable(Map<String, String> fields) {
        String rows = fields.entrySet().stream()
                .map(entry -> """
                        <tr>
                          <td style="padding:10px 12px;border-bottom:1px solid #e5e7eb;font-weight:700;vertical-align:top;width:180px;">%s</td>
                          <td style="padding:10px 12px;border-bottom:1px solid #e5e7eb;vertical-align:top;white-space:pre-wrap;">%s</td>
                        </tr>
                        """.formatted(escape(entry.getKey()), escape(entry.getValue())))
                .collect(Collectors.joining());
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" style="width:100%%;border-collapse:collapse;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
                  %s
                </table>
                """.formatted(rows);
    }

    public String note(String text) {
        return """
                <div style="margin-top:24px;padding:16px 18px;border-left:4px solid #16a34a;background:#f0fdf4;border-radius:10px;">
                  <p style="margin:0;line-height:1.6;">%s</p>
                </div>
                """.formatted(escape(text));
    }

    public String swissDateTime(Instant value) {
        if (value == null) {
            return "";
        }
        return SWISS_DATE_TIME.format(value.atZone(SWISS_ZONE));
    }

    private Map<String, String> bookingDetails(LanguageCode language, EinAb einAb, String teacherName, String teacherPhone) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put(label(language, "teacher"), teacherName);
        details.put(label(language, "teacherPhone"), valueOrDash(teacherPhone));
        details.put(label(language, "category"), categoryLabel(language, einAb.getCategory()));
        details.put(label(language, "start"), swissDateTime(einAb.getStartDateTime()));
        details.put(label(language, "location"), valueOrDash(einAb.getLocation()));
        details.put(label(language, "publicLocation"), valueOrDash(einAb.getPublicLocation()));
        details.put(label(language, "whatToBring"), valueOrDash(einAb.getWhatToBring()));
        details.put(label(language, "hint"), valueOrDash(einAb.getHint()));
        details.put(label(language, "fairteiler"), yesNo(language, einAb.isVisitFairteiler()));
        return details;
    }

    private String greeting(LanguageCode language, String name) {
        return switch (language) {
            case EN -> "Hello " + name + ",";
            case GWS -> "Hoi " + name + ",";
            case DE -> "Hallo " + name + ",";
        };
    }

    private String yesNo(LanguageCode language, boolean value) {
        return switch (language) {
            case EN -> value ? "Yes" : "No";
            case GWS -> value ? "Jo" : "Nei";
            case DE -> value ? "Ja" : "Nein";
        };
    }

    private String label(LanguageCode language, String key) {
        return switch (key) {
            case "teacher" -> switch (language) {
                case EN -> "EinAb giver";
                case GWS -> "EinAb-Geberin";
                case DE -> "EinAb-Geberin";
            };
            case "teacherPhone" -> switch (language) {
                case EN -> "Phone";
                case GWS -> "Telefon";
                case DE -> "Telefon";
            };
            case "category" -> switch (language) {
                case EN -> "Category";
                case GWS -> "Kategorie";
                case DE -> "Kategorie";
            };
            case "start" -> switch (language) {
                case EN -> "Start";
                case GWS -> "Start";
                case DE -> "Beginn";
            };
            case "location" -> switch (language) {
                case EN -> "Location";
                case GWS -> "Ort";
                case DE -> "Ort";
            };
            case "publicLocation" -> switch (language) {
                case EN -> "Public location";
                case GWS -> "Öffentleche Ort";
                case DE -> "Öffentlicher Ort";
            };
            case "whatToBring" -> switch (language) {
                case EN -> "What to bring";
                case GWS -> "Was mibringe";
                case DE -> "Was mitbringen";
            };
            case "hint" -> switch (language) {
                case EN -> "Hint";
                case GWS -> "Hiwis";
                case DE -> "Hinweis";
            };
            case "fairteiler" -> switch (language) {
                case EN -> "Fairteiler visit";
                case GWS -> "Fairteiler-Bsuech";
                case DE -> "Fairteiler-Besuch";
            };
            default -> key;
        };
    }

    private String categoryLabel(LanguageCode language, EinAbCategory category) {
        return switch (category) {
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

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
