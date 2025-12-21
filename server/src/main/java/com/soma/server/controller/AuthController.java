package com.soma.server.controller;

import com.soma.server.entity.User;
import com.soma.server.repository.UserRepository;
import com.soma.server.service.EmailService;
import com.soma.server.service.JwtService;
import com.soma.server.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/soma/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
@Slf4j
public class AuthController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Operation(
        summary = "Register a new user with email and password",
        description = "Creates a new user account and returns JWT tokens. Use the access token for subsequent authenticated requests."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful", 
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = registrationService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            // Generate and send verification email
            String verificationToken = emailService.generateVerificationToken(user);
            emailService.sendVerificationEmail(user, verificationToken);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("User registered: {}", user.getEmail());

            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .hasSpotifyConnected(false)
                    .emailVerified(false)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Registration failed"
            ));
        }
    }

    @Operation(
        summary = "Login with email and password",
        description = "Authenticates user credentials and returns JWT tokens. Use the access token in the Authorization header for protected endpoints."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful", 
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            
            if (userOptional.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid email or password"
                ));
            }

            User user = userOptional.get();

            if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid email or password"
                ));
            }

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            boolean hasSpotify = !user.getAllUserDetails().stream()
                    .filter(d -> d instanceof com.soma.server.entity.SpotifyUserDetails)
                    .toList()
                    .isEmpty();

            log.info("User logged in: {}", user.getEmail());

            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .hasSpotifyConnected(hasSpotify)
                    .emailVerified(user.isEmailVerified())
                    .build());

        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Login failed"
            ));
        }
    }

    @Operation(
        summary = "Refresh access token using refresh token",
        description = "Exchanges a valid refresh token for new access and refresh tokens. Use this when the access token expires."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            if (!jwtService.isTokenValid(refreshToken)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid or expired refresh token"
                ));
            }

            String userId = jwtService.extractUserId(refreshToken);
            Optional<User> userOptional = userRepository.findById(Long.parseLong(userId));
            
            if (userOptional.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "User not found"
                ));
            }

            User user = userOptional.get();
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken
            ));

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Token refresh failed"
            ));
        }
    }

    @Operation(
        summary = "Get current user info from JWT",
        description = "Returns the authenticated user's profile information based on the JWT access token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User information returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated or invalid token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Not authenticated"
                ));
            }

            String token = authHeader.substring(7);
            
            if (!jwtService.isTokenValid(token)) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Invalid or expired token"
                ));
            }

            String userId = jwtService.extractUserId(token);
            Optional<User> userOptional = userRepository.findById(Long.parseLong(userId));
            
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "User not found"
                ));
            }

            User user = userOptional.get();
            boolean hasSpotify = !user.getAllUserDetails().stream()
                    .filter(d -> d instanceof com.soma.server.entity.SpotifyUserDetails)
                    .toList()
                    .isEmpty();

            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "role", user.getRole().name(),
                    "hasSpotifyConnected", hasSpotify,
                    "emailVerified", user.isEmailVerified()
            ));

        } catch (Exception e) {
            log.error("Get current user failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to get user info"
            ));
        }
    }

    @Operation(
        summary = "Verify email address",
        description = "Verifies user's email address using the token sent via email"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean verified = emailService.verifyEmail(token);
        
        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired verification token"
            ));
        }
    }

    @Operation(
        summary = "Resend verification email",
        description = "Resends the verification email to the specified address"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification email sent"),
        @ApiResponse(responseCode = "400", description = "Email not found or already verified")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerificationRequest request) {
        boolean sent = emailService.resendVerificationEmail(request.getEmail());
        
        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification email sent"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email not found or already verified"
            ));
        }
    }

    // Request/Response DTOs
    @Data
    @Schema(description = "User registration request")
    public static class RegisterRequest {
        @Schema(description = "Username (3-50 characters)", example = "john_doe")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        private String username;

        @Schema(description = "Email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @Schema(description = "Password (min 6 characters)", example = "securePassword123")
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    @Data
    @Schema(description = "User login request")
    public static class LoginRequest {
        @Schema(description = "Registered email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @Schema(description = "User password", example = "securePassword123")
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Schema(description = "Token refresh request")
    public static class RefreshRequest {
        @Schema(description = "Valid refresh token from login/register response")
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    @lombok.Builder
    @Schema(description = "Authentication response with JWT tokens and user info")
    public static class AuthResponse {
        @Schema(description = "JWT access token (24h validity)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;
        
        @Schema(description = "JWT refresh token (7 days validity)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String refreshToken;
        
        @Schema(description = "Internal user ID", example = "12345")
        private Long userId;
        
        @Schema(description = "Username", example = "john_doe")
        private String username;
        
        @Schema(description = "User email", example = "user@example.com")
        private String email;
        
        @Schema(description = "User role", example = "USER", allowableValues = {"USER", "PREMIUM", "ADMIN"})
        private String role;
        
        @Schema(description = "Whether Spotify account is connected")
        private boolean hasSpotifyConnected;
        
        @Schema(description = "Whether email is verified")
        private boolean emailVerified;
    }

    @Data
    @Schema(description = "Resend verification email request")
    public static class ResendVerificationRequest {
        @Schema(description = "Email address to resend verification to")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }
}

