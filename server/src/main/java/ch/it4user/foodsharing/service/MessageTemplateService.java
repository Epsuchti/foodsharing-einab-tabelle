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

@Service
public class MessageTemplateService {

    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter SWISS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String loginSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "Your Foodsharing EinAB Tool Login Link";
            case GWS -> "Din Foodsharing EinAB Tool Login-Link";
            case DE -> "Dein Foodsharing EinAB Tool Login-Link";
        };
    }

    public String loginBody(LanguageCode language, String loginLink) {
        return String.join("\n\n",
                greeting(language),
                switch (language) {
                    case EN -> "Open this link to sign in:";
                    case GWS -> "Mach dä Link uf zum Ilogge:";
                    case DE -> "Öffne diesen Link zum Einloggen:";
                },
                switch (language) {
                    case EN -> "Sign in: " + loginLink;
                    case GWS -> "Ilogge: " + loginLink;
                    case DE -> "Einloggen: " + loginLink;
                },
                switch (language) {
                    case EN -> "If you did not request this message, you can ignore it.";
                    case GWS -> "Wenn du die Nachricht nid aagforderet hesch, chasch si eifach ignoriere.";
                    case DE -> "Wenn du diese Nachricht nicht angefordert hast, kannst du sie ignorieren.";
                });
    }

    public String bookingConfirmationSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "Your Foodsharing EinAb details";
            case GWS -> "Dini Foodsharing-EinAb";
            case DE -> "Dein Foodsharing-EinAb";
        };
    }

    public String bookingConfirmationBody(LanguageCode language, Slot slot, String manageUrl, long confirmWithinMinutes) {
        Map<String, String> details = bookingDetails(language, slot.getEinAb(), slot.getEinAb().getTeacher().getName(), slot.getEinAb().getTeacher().getPhoneNumber());
        return String.join("\n\n",
                greeting(language, slot.getBookingUser().getName()),
                confirmWindowText(language, confirmWithinMinutes),
                formatDetails(details),
                switch (language) {
                    case EN -> "You can log in later with the Foodsharing ID you used for this booking.";
                    case GWS -> "Du chasch di spöter mit dr Foodsharing-ID vo dere Buechig ilogge.";
                    case DE -> "Du kannst dich später mit der Foodsharing-ID dieser Buchung einloggen.";
                },
                switch (language) {
                    case EN -> "Confirm EinAb: " + manageUrl;
                    case GWS -> "EinAb bestätige: " + manageUrl;
                    case DE -> "EinAb bestätigen: " + manageUrl;
                });
    }

    public String teacherCancellationSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "Your EinAb was cancelled";
            case GWS -> "Dini EinAb isch abgsagt worde";
            case DE -> "Dein EinAb wurde abgesagt";
        };
    }

    public String teacherCancellationBody(LanguageCode language, Slot slot, String manageUrl) {
        Map<String, String> details = bookingDetails(language, slot.getEinAb(), slot.getEinAb().getTeacher().getName(), slot.getEinAb().getTeacher().getPhoneNumber());
        return String.join("\n\n",
                greeting(language, slot.getBookingUser().getName()),
                switch (language) {
                    case EN -> "The EinAb giver cancelled your booked EinAb.";
                    case GWS -> "D EinAb-Geberin het dis buechte EinAb abgsagt.";
                    case DE -> "Die EinAb-Geberin hat dein gebuchtes EinAb abgesagt.";
                },
                formatDetails(details),
                switch (language) {
                    case EN -> "Open bookings: " + manageUrl;
                    case GWS -> "Buechige öffne: " + manageUrl;
                    case DE -> "Buchungen öffnen: " + manageUrl;
                });
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

    private String formatDetails(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private String greeting(LanguageCode language) {
        return switch (language) {
            case EN -> "Hello,";
            case GWS -> "Hoi,";
            case DE -> "Hallo,";
        };
    }

    private String confirmWindowText(LanguageCode language, long minutes) {
        if (minutes == 60) {
            return switch (language) {
                case EN -> "Please confirm this EinAb within the next hour.";
                case GWS -> "Bitte bestätig das EinAb innerhalb vo de nächste Stund.";
                case DE -> "Bitte bestätige dieses EinAb innerhalb der nächsten Stunde.";
            };
        }
        if (minutes % 60 == 0) {
            long hours = minutes / 60;
            return switch (language) {
                case EN -> "Please confirm this EinAb within the next " + hours + (hours == 1 ? " hour." : " hours.");
                case GWS -> "Bitte bestätig das EinAb innerhalb vo de nächste " + hours + (hours == 1 ? " Stund." : " Stund.");
                case DE -> "Bitte bestätige dieses EinAb innerhalb der nächsten " + hours + (hours == 1 ? " Stunde." : " Stunden.");
            };
        }
        return switch (language) {
            case EN -> "Please confirm this EinAb within the next " + minutes + " minutes.";
            case GWS -> "Bitte bestätig das EinAb innerhalb vo de nächste " + minutes + " Minute.";
            case DE -> "Bitte bestätige dieses EinAb innerhalb der nächsten " + minutes + " Minuten.";
        };
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
}
