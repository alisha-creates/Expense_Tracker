package com.example.NexSpend.DTO.BudgetDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlertDTO {
    private String category;

    private BigDecimal budgetAmount;

    private BigDecimal spentAmount;

    private BigDecimal remaining;

    private BigDecimal utilizationPercentage;

    private String alertLevel;
}
