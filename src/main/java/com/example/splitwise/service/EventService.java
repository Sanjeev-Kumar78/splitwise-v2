package com.example.splitwise.service;

import com.example.splitwise.model.Debitor;
import com.example.splitwise.model.Event;
import com.example.splitwise.model.User;
import com.example.splitwise.repo.DebitorRepo;
import com.example.splitwise.repo.EventRepo;
import com.example.splitwise.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepo eventRepo;
    private final DebitorRepo debitorRepo;
    private final UserRepo userRepo;

    public EventService(EventRepo eventRepo, DebitorRepo debitorRepo, UserRepo userRepo){
        this.eventRepo = eventRepo;
        this.debitorRepo = debitorRepo;
        this.userRepo = userRepo;
    }

    /**
     * Create event and its splits (Debitor list should already be prepared with included users).
     * Adjusts links and saves everything in one transaction.
     */
    @Transactional
    public Event createEvent(Event e, List<Debitor> splits){
        e.setCreatedAt(LocalDateTime.now());

        // ensure each debitor links to a managed User and the event
        List<Debitor> toPersist = new ArrayList<>();
        for (Debitor d : splits){
            User u = userRepo.findById(d.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant user not found: " + d.getUser().getId()));
            d.setUser(u);
            d.setEvent(e);      // attach to the unsaved event
            toPersist.add(d);
        }

        // set splits on event (replace existing list safely)
        e.getSplits().clear();
        e.getSplits().addAll(toPersist);

        // saving event will cascade and save debitors
        return eventRepo.save(e);
    }
    @Transactional
    public Debitor addDebitor(Long eventId, Debitor d) {
        Debitor ready = addDebitorLogic(eventId, d);
        return debitorRepo.save(ready);
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsForUser(Long userId) {
        return eventRepo.findEventsByUser(userId);
    }



    @Transactional
    public void deleteDebitor(Long debitorId) {
        if (!debitorRepo.existsById(debitorId)) {
            throw new IllegalArgumentException("Debitor not found");
        }
        debitorRepo.deleteById(debitorId);
    }

    /**
     * Internal logic for validating event + user
     * and attaching the debitor to the event.
     */
    private Debitor addDebitorLogic(Long eventId, Debitor d) {

        // 1) Fetch event
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // 2) Validate user (must contain user.id)
        if (d.getUser() == null || d.getUser().getId() == null)
            throw new IllegalArgumentException("Debitor.user.id is required");

        User u = userRepo.findById(d.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 3) Attach proper references
        d.setUser(u);
        d.setEvent(e);

        // 4) Default values if null
        if (d.getDebAmount() == null) d.setDebAmount(BigDecimal.ZERO);
        if (d.getAmountPaid() == null) d.setAmountPaid(BigDecimal.ZERO);
        if (d.getPaidAt() == null) d.setPaidAt(LocalDateTime.now());
        // included default is already true
        // settled default is already false

        // 5) Add to event list in memory (optional but cleaner)
        e.getSplits().add(d);

        return d;
    }



    @Transactional(readOnly = true)
    public Event getEvent(Long id){
        // prefer the fetch-join method to ensure splits.user and creator are loaded
        return eventRepo.findByIdWithSplitsAndUsers(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    }

//    @Transactional(readOnly = true)
//    public Event getEvent(Long id){
//        return eventRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));
//    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents(){
        return eventRepo.findAll();
    }

    @Transactional
    public void deleteEvent(Long id){
        if (!eventRepo.existsById(id)) throw new IllegalArgumentException("Event not found");
        eventRepo.deleteById(id); // cascades to splits (orphanRemoval)
    }

    @Transactional
    public Event cancelEvent(Long id){
        Event e = getEvent(id);
        e.setCancelled(true);
        return eventRepo.save(e);
    }

    // helper: recompute shares if you want to create splits inside service (equal split)
    @Transactional
    public List<Debitor> createEqualSplits(Event e, List<User> participants){
        int n = participants.size();
        if (n == 0) throw new IllegalArgumentException("No participants");
        BigDecimal share = e.getTotal().divide(BigDecimal.valueOf(n), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal remainder = e.getTotal().subtract(share.multiply(BigDecimal.valueOf(n)));

        for (int i = 0; i < participants.size(); i++){
            User u = participants.get(i);
            Debitor d = new Debitor();
            d.setUser(u);
            d.setEvent(e);
            BigDecimal assigned = share;
            if (i == 0 && remainder.compareTo(BigDecimal.ZERO) > 0) assigned = assigned.add(remainder);
            d.setDebAmount(assigned);
            d.setIncluded(true);
            e.getSplits().add(d);
        }
        return e.getSplits();
    }
    @Transactional
    public Event save(Event existing) {
        // ensure each split references a managed User and has the event set
        if (existing.getSplits() != null) {
            List<Debitor> fixed = new ArrayList<>();
            for (Debitor d : existing.getSplits()) {
                if (d.getUser() != null && d.getUser().getId() != null) {
                    User managed = userRepo.findById(d.getUser().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Participant user not found: " + d.getUser().getId()));
                    d.setUser(managed);
                } else {
                    // allow debitor without user? prefer to reject — safer to throw
                    throw new IllegalArgumentException("Debitor.user.id is required");
                }
                d.setEvent(existing);
                fixed.add(d);
            }
            // replace event splits safely
            existing.getSplits().clear();
            existing.getSplits().addAll(fixed);
        }
        // persist event (will cascade to debitors if mapped)
        return eventRepo.save(existing);
    }



}
