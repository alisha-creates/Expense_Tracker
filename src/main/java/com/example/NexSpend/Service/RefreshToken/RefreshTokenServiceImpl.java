package com.example.NexSpend.Service.RefreshToken;

import com.example.NexSpend.Entity.RefreshToken;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Exception.UserNotFoundException;
import com.example.NexSpend.Exception.RefreshTokenExpiredException;
import com.example.NexSpend.Repository.RefreshTokenRepository;
import com.example.NexSpend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh.expiration}")
    private Long refreshDurationMs;

    @Override
    public RefreshToken createRefreshToken(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        RefreshToken refreshToken = refreshTokenRepository
                .findByUserId(userId)
                .orElseGet(RefreshToken::new);

        refreshToken.setUser(user);

        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        refreshToken.setToken(hashToken(rawToken));

        refreshToken.setExpiryDate(
                Instant.now().plusMillis(refreshDurationMs)
        );

        refreshToken.setRawToken(rawToken);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByToken(hashToken(token));
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().isBefore(Instant.now())) {

            refreshTokenRepository.delete(token);

            throw new RefreshTokenExpiredException("Refresh token expired. Please sign in again.");
        }

        return token;
    }

    @Override
    public void deleteByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.deleteByToken(hashToken(rawToken));
    }

    @Override
    public void deleteByEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByEmail(email).ifPresent(refreshTokenRepository::deleteByUser);
    }

    @Override
    public void deleteByUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        refreshTokenRepository.deleteByUser(user);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}