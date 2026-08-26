package com.example.NexSpend.Service.Expense;

import com.example.NexSpend.DTO.ExpenseDTO.ExpenseRequestDTO;
import com.example.NexSpend.DTO.ExpenseDTO.ExpenseResponseDTO;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

public interface ExpenseService {
    ExpenseResponseDTO createExpense(ExpenseRequestDTO requestDTO);

    ExpenseResponseDTO getExpenseById(Long id);

    Page<ExpenseResponseDTO> getAllExpenses(Pageable pageable);

    ExpenseResponseDTO updateExpense(Long id,
                                     ExpenseRequestDTO requestDTO);

    void deleteExpense(Long id);

    Page<ExpenseResponseDTO> filterExpenses(Category category,
                                            ExpenseType type,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate,
                                            Pageable pageable
    );
}
