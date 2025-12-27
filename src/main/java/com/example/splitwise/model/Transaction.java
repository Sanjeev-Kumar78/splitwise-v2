package com.example.splitwise.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime ts = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    private User toUser;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    private Long eventId; // optional link to event
    private String note;

    // getters / setters
    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public LocalDateTime getTs(){ return ts; }
    public void setTs(LocalDateTime ts){ this.ts = ts; }
    public User getFromUser(){ return fromUser; }
    public void setFromUser(User fromUser){ this.fromUser = fromUser; }
    public User getToUser(){ return toUser; }
    public void setToUser(User toUser){ this.toUser = toUser; }
    public BigDecimal getAmount(){ return amount; }
    public void setAmount(BigDecimal amount){ this.amount = amount; }
    public Long getEventId(){ return eventId; }
    public void setEventId(Long eventId){ this.eventId = eventId; }
    public String getNote(){ return note; }
    public void setNote(String note){ this.note = note; }
}
