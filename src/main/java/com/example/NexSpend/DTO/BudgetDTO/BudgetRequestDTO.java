package com.example.NexSpend.DTO.BudgetDTO;

import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Validation.EnumValidator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequestDTO {
    @NotNull
    @EnumValidator(enumClass = Category.class)
    private String category;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Integer month;

    @NotNull
    private Integer year;
}
