package com.example.NexSpend.Service.Budget;

import com.example.NexSpend.DTO.BudgetDTO.BudgetAlertDTO;
import com.example.NexSpend.DTO.BudgetDTO.BudgetRequestDTO;
import com.example.NexSpend.DTO.BudgetDTO.BudgetResponseDTO;
import com.example.NexSpend.Entity.Budget;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Exception.UnauthorizedActionException;
import com.example.NexSpend.Mapper.BudgetMapper;
import com.example.NexSpend.Repository.BudgetRepository;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetServiceImpl implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetMapper budgetMapper;

    @Override
    public BudgetResponseDTO createBudget(BudgetRequestDTO dto, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Budget budget = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                user, Category.valueOf(dto.getCategory().toUpperCase()), dto.getMonth(), dto.getYear()
        ).orElse(new Budget());

        budget.setUser(user);
        budget.setCategory(Category.valueOf(dto.getCategory().toUpperCase()));
        budget.setAmount(dto.getAmount());
        budget.setMonth(dto.getMonth());
        budget.setYear(dto.getYear());

        Budget saved = budgetRepository.save(budget);
        BigDecimal spent = calculateSpent(saved);
        return budgetMapper.mapToDto(saved, spent);
    }

    @Override
    public BudgetResponseDTO updateBudget(
            Long id,
            BudgetRequestDTO dto,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );


        Budget budget = budgetRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Budget not found")
                );

        if (!budget.getUser().getId().equals(user.getId())) {

            throw new UnauthorizedActionException(
                    "Unauthorized access"
            );

        }

        budget.setCategory(
                Category.valueOf(
                        dto.getCategory().toUpperCase()
                )
        );

        budget.setAmount(dto.getAmount());

        budget.setMonth(dto.getMonth());

        budget.setYear(dto.getYear());


        Budget updated =
                budgetRepository.save(budget);


        BigDecimal spent =
                calculateSpent(updated);


        return budgetMapper.mapToDto(
                updated,
                spent
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> getUserBudgets(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return budgetRepository.findByUserAndYear(user, LocalDate.now().getYear())
                .stream()
                .map(b -> budgetMapper.mapToDto(b, calculateSpent(b)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> getCurrentMonthBudgets(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .stream()
                .map(b -> budgetMapper.mapToDto(b, calculateSpent(b)))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateSpent(Budget budget) {
        return expenseRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                budget.getUser().getId(),
                budget.getCategory(),
                budget.getMonth(),
                budget.getYear()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetAlertDTO> checkBudgetAlerts(User user) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
                user.getId(), currentMonth, currentYear);

        List<BudgetAlertDTO> alerts = new ArrayList<>();

        for (Budget budget : budgets) {
            BigDecimal spent = expenseRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                            user.getId(), budget.getCategory(), currentMonth, currentYear);

            BigDecimal utilization = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ?
                    spent.multiply(BigDecimal.valueOf(100))
                            .divide(budget.getAmount(), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            String alertLevel = "NONE";

            if (utilization.compareTo(BigDecimal.valueOf(100)) >= 0) {
                alertLevel = "CRITICAL";
            } else if (utilization.compareTo(BigDecimal.valueOf(80)) >= 0) {
                alertLevel = "WARNING";
            }

            if (!"NONE".equals(alertLevel)) {
                alerts.add(BudgetAlertDTO.builder()
                        .category(budget.getCategory().name())
                        .budgetAmount(budget.getAmount())
                        .spentAmount(spent)
                        .remaining(budget.getAmount().subtract(spent))
                        .utilizationPercentage(utilization)
                        .alertLevel(alertLevel)
                        .build());
            }
        }
        return alerts;
    }

    @Override
    public void deleteBudget(
            Long id,
            Authentication authentication) {

        String email =
                authentication.getName();


        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        Budget budget =
                budgetRepository
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Budget not found"
                                )
                        );

        if (!budget.getUser().getId().equals(user.getId())) {

            throw new UnauthorizedActionException(
                    "Unauthorized access"
            );

        }

        budget.setDeleted(true);

        budgetRepository.save(budget);
    }
}
