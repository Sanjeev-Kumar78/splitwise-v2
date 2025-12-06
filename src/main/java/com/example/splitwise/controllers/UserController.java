package com.example.splitwise.controllers;

import com.example.splitwise.model.User;
import com.example.splitwise.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder){
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // health
    @GetMapping("/ping")
    public ResponseEntity<String> ping(){ return ResponseEntity.ok("everything okey..."); }

    // Create user (encodes password if provided)
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User u){
        if (u.getEmail() == null || u.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error","email_required"));
        }
        if (userService.findByEmail(u.getEmail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","email_exists"));
        }

        // set encoded password if provided (otherwise leave null)
        if (u.getPassword() != null && !u.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
        }

        User saved = userService.createUser(u);
        // do not return password field in response
        Map<String,Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("email", saved.getEmail());
        resp.put("username", saved.getUsername());
        resp.put("total", saved.getTotal());

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // Get one user (minimal view)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id){
        var users = userService.getAllUsers();

        for (var u : users) {
            if (id.equals(u.getId())) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("username", u.getUsername());
                m.put("total", u.getTotal());
                return ResponseEntity.ok(m);
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","user not found"));
    }

    // List users (minimal)
    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> getAllUsers(){
        var users = userService.getAllUsers();
        var list = users.stream().map(u -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("total", u.getTotal());
            return m;
        }).toList();
        return ResponseEntity.ok(list);
    }

    // Update user (partial safe update)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User payload){
        if (!userService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","user not found"));
        }

        return userService.getUser(id).map(existing -> {
            if (payload.getUsername() != null) existing.setUsername(payload.getUsername());
            if (payload.getTotal() != null) existing.setTotal(payload.getTotal());
            // if password is provided here, encode it (optional: allow admin to change)
            if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(payload.getPassword()));
            }
            User updated = userService.updateUser(existing);

            Map<String,Object> resp = new HashMap<>();
            resp.put("id", updated.getId());
            resp.put("username", updated.getUsername());
            resp.put("total", updated.getTotal());
            return ResponseEntity.ok(resp);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","user not found")));
    }

    // Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id){
        if (!userService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","user not found"));
        }
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error","delete_failed","message", ex.getMessage()));
        }
    }

    // Authenticated user's profile (works with JWT; Principal.getName() is email)
    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        String email = principal.getName();
        if (email == null) return ResponseEntity.status(400).body(Map.of("error","no_email_in_principal"));

        User u = userService.getUserWithCollectionsByEmail(email);
        if (u == null) return ResponseEntity.status(404).body(Map.of("error","user not found"));

        BigDecimal youOwe = userService.computeYouOwe(u);
        BigDecimal owedToYou = userService.computeOwedToYou(u);
        BigDecimal total = u.getTotal() == null ? BigDecimal.ZERO : u.getTotal();

        var debitors = u.getDebitors().stream()
                .map(d -> Map.of(
                        "id", d.getId(),
                        "eventId", d.getEvent() != null ? d.getEvent().getId() : null,
                        "userId", d.getUser() != null ? d.getUser().getId() : null,
                        "debAmount", d.getDebAmount(),
                        "amountPaid", d.getAmountPaid(),
                        "remaining", d.getDebAmount().subtract(d.getAmountPaid()),
                        "included", d.isIncluded(),
                        "settled", d.isSettled()
                ))
                .toList();

        var events = u.getEvents().stream()
                .map(e -> Map.of(
                        "id", e.getId(),
                        "title", e.getTitle(),
                        "total", e.getTotal(),
                        "cancelled", e.isCancelled()
                ))
                .toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", u.getId());
        resp.put("email", u.getEmail());
        resp.put("username", u.getUsername());
        resp.put("total", total);
        resp.put("emailVerified", u.isEmailVerified());
        resp.put("youOwe", youOwe);
        resp.put("owedToYou", owedToYou);
        resp.put("debitors", debitors);
        resp.put("events", events);

        return ResponseEntity.ok(resp);
    }

    // Set username for authenticated user
    @PostMapping("/set-username")
    public ResponseEntity<?> setUsername(Principal principal, @RequestBody Map<String,String> body) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error","not_authenticated"));

        String email = principal.getName();
        String username = body.get("username");

        try {
            User updated = userService.setUsernameForEmail(email, username);
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "username", updated.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // Change password for authenticated user
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(Principal principal, @RequestBody Map<String,String> body) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error","not_authenticated"));

        String email = principal.getName();
        String oldPw = body.get("oldPassword");
        String newPw = body.get("newPassword");

        if (newPw == null || newPw.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error","new_password_too_short"));
        }

        User u = userService.findByEmail(email);
        if (u == null) return ResponseEntity.status(404).body(Map.of("error","user not found"));

        // if user had no password (OAuth user converted), allow set when oldPw is absent
        if (u.getPassword() == null || u.getPassword().isBlank()) {
            // set new password directly
            u.setPassword(passwordEncoder.encode(newPw));
            userService.updateUser(u);
            return ResponseEntity.ok(Map.of("status","password_set"));
        }

        // verify old password
        if (oldPw == null || !passwordEncoder.matches(oldPw, u.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error","invalid_old_password"));
        }

        // all good — update
        u.setPassword(passwordEncoder.encode(newPw));
        userService.updateUser(u);
        return ResponseEntity.ok(Map.of("status","password_changed"));
    }

    // Search by username (case-insensitive)
    @GetMapping("/search")
    public ResponseEntity<?> searchByUsername(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }

        var opt = userService.findByUsernameIgnoreCaseOptional(username.trim());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }
        User u = opt.get();
        Map<String,Object> resp = Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "total", u.getTotal()
        );
        return ResponseEntity.ok(resp);
    }
}
