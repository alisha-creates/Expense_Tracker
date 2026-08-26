package com.example.NexSpend.Mapper;

import com.example.NexSpend.DTO.ExpenseDTO.ExpenseResponseDTO;
import com.example.NexSpend.Entity.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {
    public ExpenseResponseDTO mapToDto(Expense expense) {
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .category(expense.getCategory().name())
                .type(expense.getType().name())
                .date(expense.getDate())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
