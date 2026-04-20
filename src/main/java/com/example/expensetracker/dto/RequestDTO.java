package com.example.expensetracker.dto;

import com.example.expensetracker.entity.Category;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String description;

    @NotNull(message = "Date is required")
    private LocalDateTime date;

    @NotNull(message = "Category is required")
    private Category category;
}
