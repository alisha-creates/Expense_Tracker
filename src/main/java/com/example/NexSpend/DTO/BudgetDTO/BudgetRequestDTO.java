package com.example.NexSpend.DTO.BudgetDTO;

import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Validation.EnumValidator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    private Integer year;
}
