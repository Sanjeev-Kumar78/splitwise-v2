package com.example.splitwise.controllers;

import com.example.splitwise.model.User;
import com.example.splitwise.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService){ this.userService = userService; }

    // Create user
    @GetMapping("/ping")
    public ResponseEntity<String> ping(){ return ResponseEntity.ok("everything okey..."); }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User u){
        User saved = userService.createUser(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Get one user
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

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    // List users
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


    // Update user (full replace)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User payload){
        // cheap existence check — avoids initializing collections
        if (!userService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","user not found"));
        }

        // now load the entity (this will initialize collections inside service.getUser)
        return userService.getUser(id).map(existing -> {
            // only update allowed simple fields (defensive)
            if (payload.getUsername() != null) existing.setUsername(payload.getUsername());
            if (payload.getTotal() != null) existing.setTotal(payload.getTotal());
            User updated = userService.updateUser(existing);

            // return minimal JSON to avoid serializing lazy collections
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

//      this will show after user login via oauth .... leads his details
@GetMapping("/me")
public ResponseEntity<?> me(@AuthenticationPrincipal OAuth2User principal) {
    if (principal == null) return ResponseEntity.status(401).build();

    String email = principal.getAttribute("email");
    if (email == null) return ResponseEntity.status(400).body("No email in principal");

    User u = userService.getUserWithCollectionsByEmail(email);
    if (u == null) return ResponseEntity.status(404).body("User not found");

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

    @PostMapping("/set-username")
    public ResponseEntity<?> setUsername(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String,String> body
    ) {
        if (principal == null) return ResponseEntity.status(401).body("Not authenticated");

        String email = principal.getAttribute("email");
        String username = body.get("username");

        try {
            User updated = userService.setUsernameForEmail(email, username);
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "username", updated.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // GET /api/users/search?username=...
//    @GetMapping("/search")
//    public ResponseEntity<?> searchByUsername(@RequestParam String username) {
//        if (username == null || username.trim().isEmpty()) {
//            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
//        }
//        var opt = userService.findByUsernameOptional(username.trim());
//        if (opt.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
//        }
//        User u = opt.get();
//        Map<String,Object> resp = Map.of(
//                "id", u.getId(),
//                "username", u.getUsername(),
//                "total", u.getTotal()
//        );
//        return ResponseEntity.ok(resp);
//    }




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
