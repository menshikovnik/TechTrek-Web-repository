package com.startupgame.auth.controller;

import com.startupgame.auth.dto.request.LoginRequest;
import com.startupgame.auth.dto.request.RefreshTokenRequest;
import com.startupgame.auth.dto.request.RegisterRequest;
import com.startupgame.auth.dto.request.VerifyOtpRequest;
import com.startupgame.auth.dto.response.AccessTokenResponse;
import com.startupgame.auth.dto.response.AuthResponse;
import com.startupgame.auth.dto.response.RegisterResponse;
import com.startupgame.auth.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        authService.registerNewUser(request);
        return ResponseEntity.ok(new RegisterResponse("OTP was sent to email"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            AccessTokenResponse accessTokenResponse = authService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(accessTokenResponse);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authorized");
        }
        return ResponseEntity.ok("User is authorized");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            AuthResponse response = authService.verifyOtpAndGenerateTokens(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> guestLogin() {
        return ResponseEntity.ok(authService.createGuestSession());
    }

}
