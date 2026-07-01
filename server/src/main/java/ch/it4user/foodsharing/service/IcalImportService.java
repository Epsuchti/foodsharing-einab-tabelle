package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.openapi.model.IcalCandidate;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IcalImportService {

    private static final DateTimeFormatter ZULU = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

    public List<IcalCandidate> loadCandidates(String icalLink) {
        if (icalLink == null || icalLink.isBlank()) {
            return List.of();
        }

        List<IcalCandidate> candidates = new ArrayList<>();
        Instant now = Instant.now();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                URI.create(icalLink).toURL().openStream(), StandardCharsets.UTF_8))) {
            List<String> lines = unfoldLines(reader);
            Instant currentStart = null;
            String currentSummary = null;
            String currentLocation = null;
            for (String line : lines) {
                if (line.startsWith("DTSTART:")) {
                    currentStart = parseDateTime(line.substring("DTSTART:".length()));
                } else if (line.startsWith("DTSTART;")) {
                    currentStart = parseDateTime(line.substring(line.indexOf(':') + 1));
                } else if (line.startsWith("SUMMARY:")) {
                    currentSummary = line.substring("SUMMARY:".length()).trim();
                } else if (line.startsWith("LOCATION:")) {
                    currentLocation = line.substring("LOCATION:".length()).trim();
                } else if (line.startsWith("LOCATION;")) {
                    currentLocation = line.substring(line.indexOf(':') + 1).trim();
                } else if (line.equals("END:VEVENT") && currentStart != null) {
                    if (!currentStart.isBefore(now)) {
                        IcalCandidate candidate = new IcalCandidate();
                        candidate.setStartDateTime(OffsetDateTime.ofInstant(currentStart, ZoneOffset.UTC));
                        candidate.setSummary(currentSummary == null || currentSummary.isBlank() ? "Imported event" : currentSummary);
                        candidate.setLocation(currentLocation == null || currentLocation.isBlank() ? null : currentLocation);
                        candidates.add(candidate);
                    }
                    currentStart = null;
                    currentSummary = null;
                    currentLocation = null;
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return candidates;
    }

    private List<String> unfoldLines(BufferedReader reader) throws java.io.IOException {
        List<String> lines = new ArrayList<>();
        String current = null;
        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            if ((rawLine.startsWith(" ") || rawLine.startsWith("\t")) && current != null) {
                current += rawLine.substring(1);
                continue;
            }
            if (current != null) {
                lines.add(current);
            }
            current = rawLine;
        }
        if (current != null) {
            lines.add(current);
        }
        return lines;
    }

    private Instant parseDateTime(String rawValue) {
        if (rawValue.endsWith("Z")) {
            return OffsetDateTime.of(LocalDateTime.parse(rawValue, ZULU), ZoneOffset.UTC).toInstant();
        }
        return LocalDateTime.parse(rawValue, LOCAL).atZone(ZURICH).toInstant();
    }
}
