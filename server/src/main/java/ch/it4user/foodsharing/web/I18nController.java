package ch.it4user.foodsharing.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class I18nController {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("de", "en", "gws");

    @GetMapping(value = "/i18n/{language}.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> getTranslations(@PathVariable String language) {
        String normalizedLanguage = language.trim().toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(normalizedLanguage)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ClassPathResource resource = new ClassPathResource("static/i18n/" + normalizedLanguage + ".json");
        if (!resource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(resource);
    }
}
