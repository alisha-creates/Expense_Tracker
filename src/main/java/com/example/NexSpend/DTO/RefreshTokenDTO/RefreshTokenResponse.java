package com.example.NexSpend.DTO.RefreshTokenDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {
    private String accessToken;

    private String refreshToken;

    private String tokenType;
}
