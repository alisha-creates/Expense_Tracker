package com.example.NexSpend.DTO.BudgetDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponseDTO {
    private Long id;

    private String category;

    private BigDecimal amount;

    private BigDecimal spent;

    private BigDecimal remaining;

    private Integer month;

    private Integer year;
}
