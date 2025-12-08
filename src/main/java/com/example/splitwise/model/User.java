package com.example.splitwise.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    // materialized balance (optional). Use BigDecimal for money.
    @Column(precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "mail_given")
    private boolean EmailVerified;

    @Column(name = "email")
    private String email;

    @Column(name = "verificationToken")
    private String verificationToken;

    @Column(name = "verification_expires_at")

    private java.time.LocalDateTime verificationExpiresAt;

    @Column(nullable = false)
    private String password;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "user-debitors")
    private List<Debitor> debitors = new ArrayList<>();

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "user-events")
    private List<Event> events = new ArrayList<>();



    // helpers

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void addDebitor(Debitor d){
        debitors.add(d);
        d.setUser(this);
    }
    public void removeDebitor(Debitor d){
        debitors.remove(d);
        d.setUser(null);
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public void addEvent(Event e){
        events.add(e);
        e.setCreator(this);
    }
    public void removeEvent(Event e){
        events.remove(e);
        e.setCreator(null);
    }

    public void setEmailVerified(boolean emailVerified) {
        EmailVerified = emailVerified;
    }

    public void setDebitors(List<Debitor> debitors) {
        this.debitors = debitors;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public boolean isEmailVerified() {
        return EmailVerified;
    }

    // getters / setters (add or generate)
    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getUsername(){ return username; }
    public void setUsername(String username){ this.username = username; }
    public BigDecimal getTotal(){ return total; }
    public void setTotal(BigDecimal total){ this.total = total; }
    public List<Debitor> getDebitors(){ return debitors; }
    public List<Event> getEvents(){ return events; }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public java.time.LocalDateTime getVerificationExpiresAt() {
        return verificationExpiresAt;
    }

    public void setVerificationExpiresAt(java.time.LocalDateTime verificationExpiresAt) {
        this.verificationExpiresAt = verificationExpiresAt;
    }


}
