package com.auth_service.auth_service.repository;

import com.auth_service.auth_service.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByEmail(String email);

    // SELECT * FROM users WHERE email = :email

    boolean existsByEmail(String email);
    /**
     * 
     * SELECT COUNT(u) > 0
     * FROM UserCredential u
     * WHERE u.email = :email
     * 
     * // if user alreasy exist, count returns 1 and 1>0, it retursn true that user
     * already exist
     * 
     */
}