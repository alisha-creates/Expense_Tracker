package com.example.NexSpend.Service.RefreshToken;

import com.example.NexSpend.Entity.RefreshToken;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Exception.UserNotFoundException;
import com.example.NexSpend.Repository.RefreshTokenRepository;
import com.example.NexSpend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        /*
         * Check whether this user already has a refresh token.
         * Since user_id is unique in refresh_token table,
         * we update the existing token instead of creating
         * another row.
         */
        RefreshToken refreshToken = refreshTokenRepository
                .findByUserId(userId)
                .orElseGet(RefreshToken::new);

        // Set / update the user
        refreshToken.setUser(user);

        // Generate a new refresh token
        refreshToken.setToken(UUID.randomUUID().toString());

        // Set new expiry time
        refreshToken.setExpiryDate(
                Instant.now().plusMillis(refreshDurationMs)
        );

        // Save existing token or create a new one
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().isBefore(Instant.now())) {

            refreshTokenRepository.delete(token);

            throw new RuntimeException("Refresh token expired");
        }

        return token;
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
}