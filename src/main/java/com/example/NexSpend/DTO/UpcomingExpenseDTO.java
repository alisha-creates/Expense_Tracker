package com.example.NexSpend.DTO;

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
public class UpcomingExpenseDTO {
    private String description;
    private BigDecimal amount;
    private String category;
    private String type;
    private String frequency;
    private LocalDateTime nextDueDate;
    private int daysUntilDue;
}
