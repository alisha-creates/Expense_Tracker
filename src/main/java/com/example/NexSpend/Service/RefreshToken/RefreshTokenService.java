package com.example.NexSpend.Service.RefreshToken;

import com.example.NexSpend.Entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);

    RefreshToken verifyExpiration(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(Long userId);

    void deleteByEmail(String email);

    void deleteByToken(String rawToken);
}
