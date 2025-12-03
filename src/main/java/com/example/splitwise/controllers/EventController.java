package com.example.splitwise.controllers;
import com.example.splitwise.model.Debitor;
import com.example.splitwise.model.Event;
import com.example.splitwise.model.User;
import com.example.splitwise.service.EventService;
import com.example.splitwise.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    public EventController(EventService eventService, UserService userService){
        this.eventService = eventService;
        this.userService = userService;
    }

    // DTOs (simple, nested)
    public static class ParticipantDto {
        public Long userId;
        public boolean included = true;
        // optional custom share omitted for simplicity
    }
    public static class CreateEventDto {
        public String title;
        public Long creatorId;
        public BigDecimal total;
        public List<ParticipantDto> participants = new ArrayList<>();
    }

    // Create event with participants (equal split among included)
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody CreateEventDto dto){
        // validate creator
        User creator = userService.getUser(dto.creatorId).orElse(null);
        if (creator == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        if (dto.total == null || dto.total.compareTo(BigDecimal.ZERO) <= 0) return ResponseEntity.badRequest().build();

        Event e = new Event();
        e.setTitle(dto.title);
        e.setCreator(creator);
        e.setTotal(dto.total);

        // build participant users list (only included)
        List<User> includedUsers = dto.participants.stream()
                .filter(p -> p.included)
                .map(p -> userService.getUser(p.userId).orElse(null))
                .collect(Collectors.toList());

        if (includedUsers.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        // create equal splits and attach to event
        eventService.createEqualSplits(e, includedUsers);

        // persist event + splits
        Event saved = eventService.createEvent(e, e.getSplits());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Get event by id
//    @GetMapping("/{id}")
//    public ResponseEntity<Event> getEvent(@PathVariable Long id){
//        try {
//            Event e = eventService.getEvent(id);
//            return ResponseEntity.ok(e);
//        } catch (IllegalArgumentException ex){
//            return ResponseEntity.notFound().build();
//        }
//    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable Long id){
        try {
            Event e = eventService.getEvent(id); // uses fetch-join in service

            var splits = e.getSplits().stream().map(d -> {
                Map<String,Object> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("debAmount", d.getDebAmount());
                m.put("amountPaid", d.getAmountPaid());
                m.put("remaining", d.getRemaining());
                m.put("included", d.isIncluded());
                m.put("settled", d.isSettled());
                m.put("paidAt", d.getPaidAt());
                // user info (may be null)
                if (d.getUser() != null) {
                    m.put("userId", d.getUser().getId());
                    m.put("username", d.getUser().getUsername());
                } else {
                    m.put("userId", null);
                    m.put("username", null);
                }
                return m;
            }).toList();

            Map<String,Object> resp = new HashMap<>();
            resp.put("id", e.getId());
            resp.put("title", e.getTitle());
            resp.put("total", e.getTotal());
            resp.put("cancelled", e.isCancelled());
            resp.put("createdAt", e.getCreatedAt());
            resp.put("creatorId", e.getCreator() != null ? e.getCreator().getId() : null);
            resp.put("creatorUsername", e.getCreator() != null ? e.getCreator().getUsername() : null);
            resp.put("splits", splits);

            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "event not found"));
        } catch (Exception ex) {
            // log for diagnosis
            ex.printStackTrace();
            Map<String,Object> err = new HashMap<>();
            err.put("error", "internal_error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<?> getEvent(@PathVariable Long id){
//        try {
//            Event e = eventService.getEvent(id); // should fetch splits + users
//
//            var splits = e.getSplits().stream().map(d -> Map.of(
//                    "id", d.getId(),
//                    "debAmount", d.getDebAmount(),
//                    "amountPaid", d.getAmountPaid(),
//                    "remaining", d.getRemaining(),
//                    "included", d.isIncluded(),
//                    "settled", d.isSettled(),
//                    "paidAt", d.getPaidAt(),
//                    "userId", d.getUser() != null ? d.getUser().getId() : null,
//                    "username", d.getUser() != null ? d.getUser().getUsername() : null
//            )).toList();
//
//            Map<String,Object> resp = Map.of(
//                    "id", e.getId(),
//                    "title", e.getTitle(),
//                    "total", e.getTotal(),
//                    "cancelled", e.isCancelled(),
//                    "createdAt", e.getCreatedAt(),
//                    "creatorId", e.getCreator() != null ? e.getCreator().getId() : null,
//                    "creatorUsername", e.getCreator() != null ? e.getCreator().getUsername() : null,
//                    "splits", splits
//            );
//
//            return ResponseEntity.ok(resp);
//        } catch (IllegalArgumentException ex){
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "event not found"));
//        } catch (Exception ex) {
//            // log stacktrace so you can debug server-side
//            ex.printStackTrace();
//            // return useful JSON for frontend (and avoid exposing sensitive info)
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "internal_error", "message", ex.getMessage()));
//        }
//    }
//

    // List events
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(){
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // Delete event (hard delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id){
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex){
            return ResponseEntity.notFound().build();
        }
    }

    // Cancel (soft delete)
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Event> cancelEvent(@PathVariable Long id){
        try {
            Event e = eventService.cancelEvent(id);
            return ResponseEntity.ok(e);
        } catch (IllegalArgumentException ex){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{eventId}/debitors")
    public ResponseEntity<?> getDebitorsByEvent(@PathVariable Long eventId) {
        Event e;
        try {
            e = eventService.getEvent(eventId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "event not found"));
        }

        var list = e.getSplits().stream()
                .map(d -> {
                    var user = d.getUser();
                    Long userId = user != null ? user.getId() : null;
                    String username = user != null ? user.getUsername() : null;

                    return Map.of(
                            "debitorId", d.getId(),
                            "userId", userId,
                            "username", username,
                            "debAmount", d.getDebAmount(),
                            "paid", d.getAmountPaid(),
                            "remaining", d.getRemaining(),
                            "included", d.isIncluded()
                    );
                })
                .toList();

        return ResponseEntity.ok(list);
    }



    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody CreateEventDto payload) {

        // load event safely
        Event existing;
        try {
            existing = eventService.getEvent(id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "event not found"));
        }

        // update only allowed fields
        if (payload.title != null) existing.setTitle(payload.title);
        if (payload.total != null) existing.setTotal(payload.total);

        // SAVE
        Event saved = eventService.save(existing);  // ← you MUST add save() in EventService if missing
        return ResponseEntity.ok(saved);
    }

}
