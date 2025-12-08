package com.example.splitwise.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.splitwise.model.User;
import com.example.splitwise.repo.UserRepo;
import com.example.splitwise.service.JwtService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Public endpoints for user registration and authentication")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(AuthenticationManager authManager, UserRepo userRepo,
            PasswordEncoder encoder, JwtService jwt) {
        this.authManager = authManager;
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Operation(summary = "Register new user", description = "Create a new user account and receive JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email and password required"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User signup details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SignupRequest.class))
            )
            @RequestBody SignupRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();
        String username = request.getUsername() != null ? request.getUsername() : email;

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_and_password_required"));
        }
        if (userRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "email_exists"));
        }

        User u = new User();
        u.setEmail(email);
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setEmailVerified(false);
        userRepo.save(u);

        String token = jwt.generateToken(u.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "User login", description = "Authenticate user credentials and receive JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email and password required"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login credentials",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @RequestBody LoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_and_password_required"));
        }

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        String token = jwt.generateToken(email);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // DTOs for request bodies
    @Schema(description = "User signup request")
    public static class SignupRequest {

        @Schema(description = "User email address", example = "john.doe@example.com", required = true)
        private String email;

        @Schema(description = "User password (min 6 characters)", example = "password123", required = true)
        private String password;

        @Schema(description = "Username (optional, defaults to email)", example = "johndoe")
        private String username;

        // Default constructor for Jackson
        public SignupRequest() {
        }

        public SignupRequest(String email, String password, String username) {
            this.email = email;
            this.password = password;
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    @Schema(description = "User login request")
    public static class LoginRequest {

        @Schema(description = "User email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;

        @Schema(description = "User password", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;

        // Default constructor for Jackson
        public LoginRequest() {
        }

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Schema(description = "Authentication response with JWT token")
    public static class AuthResponse {

        @Schema(description = "JWT Bearer token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTYxNjIzOTAyMn0.4Adcj0vCZ_89jJfF...")
        private String token;

        // Default constructor for Jackson
        public AuthResponse() {
        }

        public AuthResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
