package com.example.NexSpend.Service.Budget;

import com.example.NexSpend.DTO.BudgetDTO.BudgetAlertDTO;
import com.example.NexSpend.DTO.BudgetDTO.BudgetRequestDTO;
import com.example.NexSpend.DTO.BudgetDTO.BudgetResponseDTO;
import com.example.NexSpend.Entity.User;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface BudgetService {
    BudgetResponseDTO createOrUpdateBudget(BudgetRequestDTO dto, Authentication authentication);

    List<BudgetResponseDTO> getUserBudgets(Authentication authentication);

    List<BudgetResponseDTO> getCurrentMonthBudgets(Authentication authentication);

    List<BudgetAlertDTO> checkBudgetAlerts(User user);
}
