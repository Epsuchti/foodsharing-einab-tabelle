package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.AuthApi;
import ch.it4user.foodsharing.openapi.model.AuthResponse;
import ch.it4user.foodsharing.openapi.model.LoginRequest;
import ch.it4user.foodsharing.openapi.model.MessageResponse;
import ch.it4user.foodsharing.openapi.model.VerifyTokenRequest;
import ch.it4user.foodsharing.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<MessageResponse> requestLogin(String bezirkSlug, LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.requestLogin(bezirkSlug, loginRequest.getFoodsharingId()));
    }

    @Override
    public ResponseEntity<AuthResponse> verifyLoginToken(VerifyTokenRequest verifyTokenRequest) {
        return ResponseEntity.ok(authService.verifyToken(verifyTokenRequest.getToken()));
    }
}
