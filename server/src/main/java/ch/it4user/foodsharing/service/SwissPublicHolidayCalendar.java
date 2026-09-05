package ch.it4user.foodsharing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SwissPublicHolidayCalendar {
    private static final String API_URL = "https://nagerholidays.com/api/v4/Holidays/CH/";
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;
    private final Map<Integer, Map<LocalDate, String>> calendars = new ConcurrentHashMap<>();

    public SwissPublicHolidayCalendar(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> holidayName(LocalDate date) {
        return Optional.ofNullable(calendar(date.getYear()).get(date));
    }

    public Map<LocalDate, String> holidays(int year) {
        return Map.copyOf(calendar(year));
    }

    private Map<LocalDate, String> calendar(int year) {
        return calendars.computeIfAbsent(year, this::load);
    }

    private Map<LocalDate, String> load(int year) {
        try {
            String response = restClient.get().uri(API_URL + year).retrieve().body(String.class);
            JsonNode holidays = objectMapper.readTree(response);
            Map<LocalDate, String> result = new ConcurrentHashMap<>();
            for (JsonNode holiday : holidays) {
                if (!holiday.path("nationalHoliday").asBoolean(false)) {
                    continue;
                }
                result.put(LocalDate.parse(holiday.required("date").asText()), holiday.path("name").asText("Feiertag"));
            }
            return result;
        } catch (Exception exception) {
            throw new PublicHolidayCalendarUnavailableException(exception);
        }
    }

    public static class PublicHolidayCalendarUnavailableException extends RuntimeException {
        PublicHolidayCalendarUnavailableException(Exception cause) {
            super(cause);
        }
    }
}
