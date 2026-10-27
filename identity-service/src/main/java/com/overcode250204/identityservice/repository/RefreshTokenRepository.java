package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.RefreshToken;
import com.overcode250204.identityservice.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Modifying
    @Query("delete from RefreshToken token where token.user = :user")
    int deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("delete from RefreshToken token where token.refreshToken = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.refreshToken = :tokenHash")
    Optional<RefreshToken> findByRefreshTokenForUpdate(@Param("tokenHash") String tokenHash);
}
