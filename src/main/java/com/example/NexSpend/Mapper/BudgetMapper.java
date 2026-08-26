package com.example.NexSpend.Mapper;

import com.example.NexSpend.DTO.BudgetDTO.BudgetResponseDTO;
import com.example.NexSpend.Entity.Budget;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BudgetMapper {
    public BudgetResponseDTO mapToDto(Budget budget, BigDecimal spent) {
        return BudgetResponseDTO.builder()
                .id(budget.getId())
                .category(budget.getCategory().name())
                .amount(budget.getAmount())
                .spent(spent)
                .remaining(budget.getAmount().subtract(spent))
                .month(budget.getMonth())
                .year(budget.getYear())
                .build();
    }
}
