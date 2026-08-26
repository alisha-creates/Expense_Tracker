package com.example.NexSpend.DTO.ExpenseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponseDTO {
    private Long id;

    private String description;

    private BigDecimal amount;

    private String category;

    private String type;

    private LocalDateTime date;

    private LocalDateTime createdAt;
}
