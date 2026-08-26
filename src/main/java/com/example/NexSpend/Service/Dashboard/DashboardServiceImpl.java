package com.example.NexSpend.Service.Dashboard;

import com.example.NexSpend.DTO.DashboardResponseDTO;
import com.example.NexSpend.DTO.ExpenseDTO.ExpenseResponseDTO;
import com.example.NexSpend.DTO.BudgetDTO.BudgetResponseDTO;
import com.example.NexSpend.DTO.UpcomingExpenseDTO;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.Expense;
import com.example.NexSpend.Entity.ExpenseType;
import com.example.NexSpend.Exception.UserNotFoundException;
import com.example.NexSpend.Mapper.ExpenseMapper;
import com.example.NexSpend.Repository.BudgetRepository;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.RecurringExpenseRepository;
import com.example.NexSpend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.example.NexSpend.Entity.User;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    private final ExpenseRepository expenseRepository;

    private final UserRepository userRepository;

    private final BudgetRepository budgetRepository;

    private final RecurringExpenseRepository recurringExpenseRepository;

    private final ExpenseMapper expenseMapper;

    @Override
    public DashboardResponseDTO getDashboard(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        Long userId = user.getId();
        BigDecimal income = expenseRepository.sumByUserIdAndType(userId, ExpenseType.INCOME);
        BigDecimal expense = expenseRepository.sumByUserIdAndType(userId, ExpenseType.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        // Today Expense
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal todayExpense = expenseRepository.sumTodayExpense(userId, startOfDay, endOfDay);

        // Monthly Expense
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);
        BigDecimal monthlyExpense = expenseRepository.sumMonthlyExpense(userId, startOfMonth, endOfMonth);

        BigDecimal savingsRate = income.compareTo(BigDecimal.ZERO) > 0 ?
                balance.multiply(BigDecimal.valueOf(100))
                        .divide(income, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Top Category
        List<Object[]> cats = expenseRepository.topCategories(userId);
        String topCategory = cats.isEmpty() ? "N/A" : ((Category) cats.get(0)[0]).name();

        // Last 10 Transactions
        List<ExpenseResponseDTO> last10 = expenseRepository
                .findTop10ByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(expenseMapper::mapToDto)
                .toList();

        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        for (Object[] row : expenseRepository.sumExpenseByCategory(user.getId())) {
            expenseByCategory.put(((Category) row[0]).name(), (BigDecimal) row[1]);
        }

        Map<String, BigDecimal> expenseTrends = new LinkedHashMap<>();
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate day = sevenDaysAgo.plusDays(dayOffset);
            expenseTrends.put(day.getDayOfWeek().name().substring(0, 3), BigDecimal.ZERO);
        }
        for (Object[] row : expenseRepository.expenseTrend(userId, sevenDaysAgo.atStartOfDay())) {
            LocalDate day = LocalDate.parse(row[0].toString());
            expenseTrends.put(day.getDayOfWeek().name().substring(0, 3), (BigDecimal) row[1]);
        }

        List<Map<String, Object>> monthlyTrends = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            LocalDateTime start = monthDate.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = monthDate.withDayOfMonth(monthDate.lengthOfMonth()).atTime(LocalTime.MAX);

            BigDecimal monthExpense = expenseRepository.sumMonthlyExpense(userId, start, end);

            Map<String, Object> trend = new HashMap<>();
            trend.put("month", monthDate.getMonth().name().substring(0, 3));
            trend.put("expense", monthExpense);
            monthlyTrends.add(trend);
        }

        List<BudgetResponseDTO> currentBudgets = budgetRepository.findByUserIdAndMonthAndYear(
                        userId, LocalDate.now().getMonthValue(), LocalDate.now().getYear())
                .stream()
                .map(budget -> {
                    BigDecimal spent = expenseRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                                    userId, budget.getCategory(),
                                    LocalDate.now().getMonthValue(), LocalDate.now().getYear());
                    return BudgetResponseDTO.builder()
                            .id(budget.getId())
                            .category(budget.getCategory().name())
                            .amount(budget.getAmount())
                            .spent(spent)
                            .remaining(budget.getAmount().subtract(spent))
                            .month(budget.getMonth())
                            .year(budget.getYear())
                            .build();
                }).toList();

        BigDecimal totalBudget = currentBudgets.stream()
                .map(BudgetResponseDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpentOnBudget = currentBudgets.stream()
                .map(BudgetResponseDTO::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal utilization = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalSpentOnBudget.multiply(BigDecimal.valueOf(100)).divide(totalBudget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<UpcomingExpenseDTO> upcoming = getUpcomingRecurringExpenses(user);

        return DashboardResponseDTO.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .balance(balance)
                .todayExpense(todayExpense)
                .monthlyExpense(monthlyExpense)
                .savingsRate(savingsRate)
                .monthlyTrends(monthlyTrends)
                .last10Transactions(last10)
                .topCategory(topCategory)
                .expenseTrends(expenseTrends)
                .expenseByCategory(expenseByCategory)
                .currentMonthBudgets(currentBudgets)
                .totalBudget(totalBudget)
                .totalSpentOnBudgetedCategories(totalSpentOnBudget)
                .budgetUtilizationPercentage(utilization)
                .upcomingRecurringExpenses(upcoming)
                .build();
    }

    private List<UpcomingExpenseDTO> getUpcomingRecurringExpenses(User user) {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime thirtyDaysLater = today.plusDays(30);

        return recurringExpenseRepository.findByUserAndActiveTrue(user)
                .stream()
                .filter(re -> !re.getNextExecutionDate().isAfter(thirtyDaysLater))
                .map(re -> {
                    int daysUntil = (int) ChronoUnit.DAYS.between(today, re.getNextExecutionDate());
                    return UpcomingExpenseDTO.builder()
                            .description(re.getDescription())
                            .amount(re.getAmount())
                            .category(re.getCategory().name())
                            .type(re.getType().name())
                            .frequency(String.valueOf(re.getFrequency()))
                            .nextDueDate(re.getNextExecutionDate())
                            .daysUntilDue(daysUntil)
                            .build();
                })
                .sorted((a, b) -> a.getNextDueDate().compareTo(b.getNextDueDate()))
                .toList();
    }
}
