package ch.it4user.foodsharing.service;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode code;
    private final List<String> details;

    public ApiException(HttpStatus status, ApiErrorCode code) {
        this(status, code, List.of());
    }

    public ApiException(HttpStatus status, ApiErrorCode code, List<String> details) {
        super(code.name());
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
