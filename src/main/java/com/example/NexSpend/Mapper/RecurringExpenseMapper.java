package com.example.NexSpend.Mapper;

import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseResponseDTO;
import com.example.NexSpend.Entity.Frequency;
import com.example.NexSpend.Entity.RecurringExpense;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

@Component
public class RecurringExpenseMapper {
    public RecurringExpenseResponseDTO mapToDto(RecurringExpense recurringExpense) {
        return RecurringExpenseResponseDTO.builder()
                .id(recurringExpense.getId())
                .description(recurringExpense.getDescription())
                .amount(recurringExpense.getAmount())
                .category(recurringExpense.getCategory().name())
                .type(recurringExpense.getType().name())
                .frequency(String.valueOf(recurringExpense.getFrequency()))
                .nextExecutionDate(recurringExpense.getNextExecutionDate())
                .active(recurringExpense.isActive())
                .build();
    }
}
