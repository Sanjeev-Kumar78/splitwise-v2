package com.example.splitwise.repo;


import com.example.splitwise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String verificationToken);


    User findUserByEmail(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}

