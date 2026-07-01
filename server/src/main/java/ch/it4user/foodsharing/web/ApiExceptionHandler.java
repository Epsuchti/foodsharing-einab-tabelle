package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.model.ErrorResponse;
import ch.it4user.foodsharing.openapi.model.ErrorCode;
import ch.it4user.foodsharing.service.ApiErrorCode;
import ch.it4user.foodsharing.service.ApiException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice(assignableTypes = {
        AuthController.class,
        PublicController.class,
        UserController.class,
        TeacherController.class,
        AdminController.class
})
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getStatus(), exception.getCode(), exception.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED, List.of()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNEXPECTED_ERROR, List.of()));
    }

    private ErrorResponse error(HttpStatus status, ApiErrorCode code, List<String> details) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setMessage(code.name());
        response.setCode(ErrorCode.fromValue(code.name()));
        response.setDetails(details);
        return response;
    }
}
