package com.example.NexSpend.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String accessToken;

    private String refreshToken;

    private String message;

    private String email;

    private String name;
}
