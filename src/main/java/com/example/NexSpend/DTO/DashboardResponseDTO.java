package com.example.NexSpend.DTO;

import com.example.NexSpend.DTO.BudgetDTO.BudgetResponseDTO;
import com.example.NexSpend.DTO.ExpenseDTO.ExpenseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDTO {
    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal balance;

    private BigDecimal todayExpense;

    private BigDecimal monthlyExpense;

    private BigDecimal savingsRate;

    List<ExpenseResponseDTO> last10Transactions;

    private String topCategory;

    private Map<String, BigDecimal> expenseTrends;

    private Map<String, BigDecimal> expenseByCategory;

    private List<Map<String, Object>> monthlyTrends;

    private List<BudgetResponseDTO> currentMonthBudgets;

    private BigDecimal totalBudget;

    private BigDecimal totalSpentOnBudgetedCategories;

    private BigDecimal budgetUtilizationPercentage;

    private List<UpcomingExpenseDTO> upcomingRecurringExpenses;
}
