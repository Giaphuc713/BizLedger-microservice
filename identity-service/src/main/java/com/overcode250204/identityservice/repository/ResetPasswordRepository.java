package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.ForgotPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface ResetPasswordRepository extends JpaRepository<ForgotPassword, UUID> {
    Optional<ForgotPassword> findByToken(String token);
}
