package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.HelloApi;
import ch.it4user.foodsharing.openapi.model.HelloResponse;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController implements HelloApi {

    @Override
    public ResponseEntity<HelloResponse> getHello() {
        return ResponseEntity.ok(new HelloResponse("Hello from Spring Boot", OffsetDateTime.now()));
    }
}
