package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class MessageTemplateService {

    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter SWISS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final MessageSource messageSource;

    public MessageTemplateService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String loginSubject(LanguageCode language) {
        return message(language, "message.login.subject");
    }

    public String loginBody(LanguageCode language, String loginLink) {
        return String.join("\n\n",
                message(language, "message.greeting"),
                message(language, "message.login.intro"),
                message(language, "message.login.link", loginLink),
                message(language, "message.login.ignore"));
    }

    public String bookingConfirmationSubject(LanguageCode language) {
        return message(language, "message.booking-confirmation.subject");
    }

    public String bookingConfirmationBody(LanguageCode language, Slot slot, String manageUrl, long confirmWithinMinutes) {
        Map<String, String> details = bookingDetails(language, slot.getEinAb(), slot.getEinAb().getTeacher().getName(), slot.getEinAb().getTeacher().getPhoneNumber());
        return String.join("\n\n",
                message(language, "message.greeting.named", slot.getBookingUser().getName()),
                confirmWindowText(language, confirmWithinMinutes),
                formatDetails(details),
                message(language, "message.booking-confirmation.login-later"),
                message(language, "message.booking-confirmation.link", manageUrl));
    }

    public String teacherCancellationSubject(LanguageCode language) {
        return message(language, "message.teacher-cancellation.subject");
    }

    public String teacherCancellationBody(LanguageCode language, Slot slot, String manageUrl) {
        Map<String, String> details = bookingDetails(language, slot.getEinAb(), slot.getEinAb().getTeacher().getName(), slot.getEinAb().getTeacher().getPhoneNumber());
        return String.join("\n\n",
                message(language, "message.greeting.named", slot.getBookingUser().getName()),
                message(language, "message.teacher-cancellation.intro"),
                formatDetails(details),
                message(language, "message.teacher-cancellation.link", manageUrl));
    }

    public String swissDateTime(Instant value) {
        return value == null ? "" : SWISS_DATE_TIME.format(value.atZone(SWISS_ZONE));
    }

    private Map<String, String> bookingDetails(LanguageCode language, EinAb einAb, String teacherName, String teacherPhone) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put(message(language, "message.details.teacher"), teacherName);
        details.put(message(language, "message.details.teacher-phone"), valueOrDash(teacherPhone));
        details.put(message(language, "message.details.category"), categoryLabel(language, einAb.getCategory()));
        details.put(message(language, "message.details.start"), swissDateTime(einAb.getStartDateTime()));
        if (einAb.getCategory() == EinAbCategory.ONLINE) {
            details.put(message(language, "message.details.online-call-link"), valueOrDash(einAb.getOnlineCallLink()));
        } else {
            details.put(message(language, "message.details.location"), valueOrDash(einAb.getLocation()));
            details.put(message(language, "message.details.public-location"), valueOrDash(einAb.getPublicLocation()));
            details.put(message(language, "message.details.what-to-bring"), valueOrDash(einAb.getWhatToBring()));
        }
        details.put(message(language, "message.details.hint"), valueOrDash(einAb.getHint()));
        details.put(message(language, "message.details.fairteiler"), message(language, einAb.isVisitFairteiler() ? "message.yes" : "message.no"));
        return details;
    }

    private String formatDetails(Map<String, String> fields) {
        return fields.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("\n"));
    }

    private String confirmWindowText(LanguageCode language, long minutes) {
        if (minutes == 60) return message(language, "message.booking-confirmation.confirm-hour");
        if (minutes % 60 == 0) return message(language, minutes == 60 ? "message.booking-confirmation.confirm-hour" : "message.booking-confirmation.confirm-hours", minutes / 60);
        return message(language, "message.booking-confirmation.confirm-minutes", minutes);
    }

    private String categoryLabel(LanguageCode language, EinAbCategory category) {
        return message(language, "message.category." + category.name().toLowerCase(Locale.ROOT));
    }

    private String message(LanguageCode language, String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, Locale.forLanguageTag(language.getCode()));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
