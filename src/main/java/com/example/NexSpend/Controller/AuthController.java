package com.example.NexSpend.Controller;

import com.example.NexSpend.DTO.RefreshTokenDTO.RefreshTokenRequest;
import com.example.NexSpend.DTO.RefreshTokenDTO.RefreshTokenResponse;
import com.example.NexSpend.Entity.RefreshToken;
import com.example.NexSpend.Exception.InvalidTokenException;
import com.example.NexSpend.Service.CustomUserDetailsService;
import com.example.NexSpend.Service.JWT.JwtService;
import com.example.NexSpend.Service.RefreshToken.RefreshTokenService;
import com.example.NexSpend.Service.User.UserService;
import com.example.NexSpend.DTO.UserDTO.UserRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserResponseDTO;
import com.example.NexSpend.DTO.LoginRequestDTO;
import com.example.NexSpend.DTO.AuthResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    private final RefreshTokenService refreshTokenService;

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid @RequestBody UserRequestDTO dto
    ) {
        UserResponseDTO response = userService.register(dto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto
    ) {
        AuthResponseDTO response = userService.login(dto);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(response);
    }

    @GetMapping("/activate")
    public ResponseEntity<Void> activateUser(
            @RequestParam String token
    ) {
        try {
            userService.activateUser(token);

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header(
                            "Location",
                            "/?activated=true"
                    )
                    .build();
        }catch (InvalidTokenException e) {

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header(
                            "Location",
                            "/?activationError=" +
                                    java.net.URLEncoder.encode(
                                            e.getMessage(),
                                            java.nio.charset.StandardCharsets.UTF_8
                                    )
                    )
                    .build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshToken token = refreshTokenService
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        refreshTokenService.verifyExpiration(token);

        UserDetails userDetails = userDetailsService
                .loadUserByUsername(token.getUser().getEmail());

        String newAccessToken = jwtService.generateToken(userDetails);
        RefreshToken rotatedToken = refreshTokenService.createRefreshToken(token.getUser().getId());

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(RefreshTokenResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(rotatedToken.getRawToken())
                        .tokenType("Bearer")
                        .build());
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        // Revocation works even when the access token has already expired.
        refreshTokenService.deleteByToken(request.getRefreshToken());

        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }
}

