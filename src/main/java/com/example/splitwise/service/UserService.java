package com.example.splitwise.service;

import com.example.splitwise.model.User;
import com.example.splitwise.repo.DebitorRepo;
import com.example.splitwise.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepo userRepo;
    @Autowired
    private final DebitorRepo debitorRepo;

    public UserService(UserRepo userRepo, DebitorRepo debitorRepo){
        this.userRepo = userRepo;
        this.debitorRepo = debitorRepo;
    }

    @Transactional
    public User createUser(User u){
        return userRepo.save(u);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUser(Long id){
        Optional<User> opt = userRepo.findById(id);
        // initialize lazy collections while we are still inside the transaction
        opt.ifPresent(user -> {
            // touch collections to load them
            user.getDebitors().size();
            user.getEvents().size();
        });
        return opt;
    }



    @Transactional(readOnly = true)
    public List<User> getAllUsers(){
        return userRepo.findAll();
    }

    @Transactional
    public User updateUser(User u){
        return userRepo.save(u);
    }
    @Transactional(readOnly = true)
    public boolean existsById(Long id){
        return userRepo.existsById(id);
    }


    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }

    @Transactional
    public void deleteUser(Long id){
        if (!userRepo.existsById(id)) throw new IllegalArgumentException("User not found");
        userRepo.deleteById(id);
    }
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (username == null) return false;
        return userRepo.existsByUsername(username);
    }

    @Transactional
    public User setUsernameForEmail(String email, String username) {
        if (email == null || username == null) throw new IllegalArgumentException("email/username required");

        // basic validation (you already had same checks in controller)
        username = username.trim();
        if (username.length() < 3) throw new IllegalArgumentException("username too short");
        if (!username.matches("^[A-Za-z0-9._]+$")) throw new IllegalArgumentException("invalid username");

        if (userRepo.existsByUsername(username)) {
            throw new IllegalStateException("username_taken");
        }

        User u = userRepo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("user_not_found"));
        u.setUsername(username);
        return userRepo.save(u);
    }

    // in UserService.java
    public Optional<User> findByEmailOptional(String email) {
        return userRepo.findByEmail(email); // implement repo method to return Optional
    }
    public Optional<User> findByUsernameOptional(String username) {
        return userRepo.findByUsername(username);
    }

    public Optional<User> findByUsernameIgnoreCaseOptional(String username) {
        return userRepo.findByUsernameIgnoreCase(username);
    }
    @Transactional(readOnly = true)
    public Optional<User> getUserWithCollections(Long id) {
        Optional<User> opt = userRepo.findById(id);
        opt.ifPresent(u -> {
            // touch collections to initialize
            u.getDebitors().size();
            u.getEvents().size();
            // For each event, ensure splits are loaded
            u.getEvents().forEach(e -> e.getSplits().size());
        });
        return opt;
    }
    @Transactional(readOnly = true)
    public User getUserWithCollectionsByEmail(String email) {
        var opt = userRepo.findByEmail(email);
        if (opt.isEmpty()) return null;
        User u = opt.get();
        // touch collections to load them
        u.getDebitors().size();
        u.getEvents().size();
        u.getEvents().forEach(e -> e.getSplits().size());
        return u;
    }
    public BigDecimal computeYouOwe(User u) {
        if (u == null) return BigDecimal.ZERO;
        return u.getDebitors().stream()
//                .filter(Debitor::isIncluded) // optional: only included
                .filter(d -> !d.isSettled())
                .map(d -> {
                    BigDecimal deb = d.getDebAmount() == null ? BigDecimal.ZERO : d.getDebAmount();
                    BigDecimal paid = d.getAmountPaid() == null ? BigDecimal.ZERO : d.getAmountPaid();
                    return deb.subtract(paid);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public Optional<User> getByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public BigDecimal computeOwedToYou(User u) {
        if (u == null) return BigDecimal.ZERO;
        return u.getEvents().stream()
                .filter(e -> !Boolean.TRUE.equals(e.isCancelled()))
                .map(e -> {
                    BigDecimal total = e.getTotal() == null ? BigDecimal.ZERO : e.getTotal();
                    BigDecimal paid = e.getSplits().stream()
                            .map(s -> s.getAmountPaid() == null ? BigDecimal.ZERO : s.getAmountPaid())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return total.subtract(paid);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }



}
