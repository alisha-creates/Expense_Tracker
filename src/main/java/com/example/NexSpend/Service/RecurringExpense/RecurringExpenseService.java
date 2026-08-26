package com.example.NexSpend.Service.RecurringExpense;

import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseRequestDTO;
import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
public interface RecurringExpenseService {
    RecurringExpenseResponseDTO createRecurring(RecurringExpenseRequestDTO dto, Authentication auth);
    List<RecurringExpenseResponseDTO> getUserRecurring(Authentication auth);
    void processDueRecurringExpenses();
}
