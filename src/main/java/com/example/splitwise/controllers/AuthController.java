package com.example.splitwise.controllers;

import com.example.splitwise.model.User;
import com.example.splitwise.repo.UserRepo;
//import com.example.splitwise.JwtService;
import com.example.splitwise.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String username = body.getOrDefault("username", email);

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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error","email_and_password_required"));
        }

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
        }

        String token = jwt.generateToken(email);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
