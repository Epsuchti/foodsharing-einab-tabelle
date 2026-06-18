package ch.it4user.foodsharing.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class I18nController {

    @GetMapping(value = "/i18n/{language}.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTranslations(@PathVariable String language) throws IOException {
        ClassPathResource resource = new ClassPathResource("static/i18n/" + language + ".json");
        if (!resource.exists()) {
            throw new ResponseStatusException(NOT_FOUND, "Translation file not found");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        }
    }
}
