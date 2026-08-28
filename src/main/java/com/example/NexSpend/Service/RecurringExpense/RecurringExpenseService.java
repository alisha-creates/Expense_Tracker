package com.example.NexSpend.Service.RecurringExpense;

import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseRequestDTO;
import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
public interface RecurringExpenseService {
    RecurringExpenseResponseDTO createRecurring(RecurringExpenseRequestDTO dto, Authentication auth);

    List<RecurringExpenseResponseDTO> getUserRecurring(Authentication auth);

    RecurringExpenseResponseDTO updateRecurring(
            Long id,
            RecurringExpenseRequestDTO dto,
            Authentication authentication
    );

    void deleteRecurring(
            Long id,
            Authentication authentication
    );

    void processDueRecurringExpenses();
}
