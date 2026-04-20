package com.example.expensetracker.mapper;

import com.example.expensetracker.dto.*;
import com.example.expensetracker.entity.Expense;

public class Mapper {
    public static Expense toEntity(RequestDTO dto) {
        return Expense.builder()
                .title(dto.getTitle())
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .date(dto.getDate())
                .category(dto.getCategory())
                .build();
    }

    public static ResponseDTO toDTO(Expense expense) {
        return ResponseDTO.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .date(expense.getDate())
                .createdAt(expense.getCreatedAt())
                .category(expense.getCategory())
                .build();
    }
}
