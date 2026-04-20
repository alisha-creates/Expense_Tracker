package com.example.expensetracker.dto;

import com.example.expensetracker.entity.Category;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO {
    private Long id;
    private String title;
    private Double amount;
    private Category category;
    private String description;
    private LocalDateTime date;
    private LocalDateTime createdAt;
}
