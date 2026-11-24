package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);

    Optional<PasswordResetToken> findFirstByUserIdAndTokenHashAndUsedFalseOrderByCreatedAtDesc(
            Long userId, String tokenHash);
}
