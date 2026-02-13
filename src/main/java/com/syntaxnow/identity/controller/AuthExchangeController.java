package com.syntaxnow.identity.controller;

import com.google.firebase.auth.FirebaseToken;
import com.syntaxnow.identity.dto.TokenExchangeResponse;
import com.syntaxnow.identity.service.FirebaseTokenService;
import com.syntaxnow.identity.service.KeycloakAdminService;
import com.syntaxnow.identity.service.KeycloakTokenService;
import com.syntaxnow.identity.utils.PasswordGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthExchangeController {

    private final FirebaseTokenService firebase;
    private final KeycloakAdminService keycloakAdmin;
    private final KeycloakTokenService keycloakToken;

    @PostMapping("/exchange")
    public TokenExchangeResponse exchange(
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {
        String firebaseToken = authHeader.replace("Bearer ", "");
        FirebaseToken decoded = firebase.verifyOtpToken(firebaseToken);
        String phone = firebase.extractPhone(decoded);
        String adminToken = keycloakAdmin.getAdminToken();
        String userId = keycloakAdmin.findUserByPhone(phone, adminToken);
        if (userId == null) {
            userId = keycloakAdmin.createUser(phone, adminToken);
        }

        // Set password (MVP login bridge)
        String tempPassword = PasswordGenerator.generate();
        keycloakAdmin.setUserPassword(userId, tempPassword, adminToken);

        // Login user → Get access + refresh token
        Map<String, Object> tokenResponse =
                keycloakToken.loginUser(phone, tempPassword);


        if (!tokenResponse.containsKey("access_token")) {
            throw new IllegalStateException("Invalid token response from Keycloak");
        }

        return new TokenExchangeResponse(
                userId,
                phone,
                tokenResponse.get("access_token").toString(),
                tokenResponse.get("refresh_token").toString(),
                (int) tokenResponse.get("expires_in")
        );
    }

    @PostMapping("/exchange/google")
    public TokenExchangeResponse exchangeGoogle(
            @RequestBody Map<String, String> body
    ) {
        String code = body.get("code");
        return keycloakToken.exchangeAuthorizationCode(code);
    }

    /**
     * Do NOT pass Firebase token here
     * This endpoint must be public
     * @param authHeader
     * @return
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenExchangeResponse> refresh(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String refreshToken = authHeader.substring(7);

        TokenExchangeResponse refreshed =
                keycloakToken.refresh(refreshToken);

        return ResponseEntity.ok(refreshed);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        String refreshToken = authHeader.substring(7);
        keycloakToken.logout(refreshToken);
        return ResponseEntity.ok().build();
    }
}
