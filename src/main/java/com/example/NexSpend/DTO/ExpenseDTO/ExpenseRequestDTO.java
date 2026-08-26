package com.example.NexSpend.DTO.ExpenseDTO;

import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.ExpenseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.example.NexSpend.Validation.EnumValidator;
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
public class ExpenseRequestDTO {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;

    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();

    @NotNull(message = "Category is required")
    @EnumValidator(enumClass = Category.class, message = "Invalid category")
    private String category;

    @NotNull(message = "ExpenseType in required")
    @EnumValidator(enumClass = ExpenseType.class, message = "Invalid expense type")
    private String type;
}
