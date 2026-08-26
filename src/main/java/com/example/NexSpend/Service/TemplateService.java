package com.example.NexSpend.Service;

import com.example.NexSpend.DTO.BudgetDTO.BudgetAlertDTO;
import com.example.NexSpend.Entity.Expense;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TemplateService {
    public String buildReminder() {

        return """
        <div style='font-family:Arial;padding:20px'>
            <h2 style='color:#4CAF50;'>NexSpend Reminder</h2>
            <p>Please add today's expenses.</p>
        </div>
        """;
    }

    public String buildWeeklySummary(List<Expense> expenses, BigDecimal totalExpense, BigDecimal totalIncome) {

        StringBuilder rows = new StringBuilder();
        for (Expense e : expenses) {
            rows.append("<tr>")
                    .append("<td>")
                    .append(e.getCategory() != null
                            ? e.getCategory().name()
                            : "NA")
                    .append("</td>")

                    .append("<td>")
                    .append(e.getType().name())
                    .append("</td>")

                    .append("<td>₹")
                    .append(e.getAmount())
                    .append("</td>")

                    .append("<td>")
                    .append(e.getDate())
                    .append("</td>")
                    .append("</tr>");
        }
        return """
        <div style='font-family:Arial;padding:20px'>

            <h2 style='color:#2196F3;'>
                Weekly Expense Summary
            </h2>

            <table border='1'
                   cellspacing='0'
                   cellpadding='10'
                   style='border-collapse:collapse;width:100%'>

                <tr style='background:#f2f2f2'>
                    <th>Category</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Date</th>
                </tr>
        """
                + rows +
                """
            </table>

            <br>

            <h3>Total Income: ₹
        """
                + totalIncome +
                """
            </h3>

            <h3>Total Expense: ₹
        """
                + totalExpense +
                """
            </h3>

            <h3>Balance: ₹
        """
                + (totalIncome.subtract(totalExpense)) +
                """
            </h3>

        </div>
        """;
    }

    public String buildBudgetAlert(List<BudgetAlertDTO> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <div style='font-family:Arial;padding:20px'>
                <h2 style='color:#FF5722;'>⚠️ Budget Alert</h2>
                <p>You have exceeded or are close to exceeding your budget in the following categories:</p>
                <table border='1' cellspacing='0' cellpadding='10' style='border-collapse:collapse;width:100%'>
                    <tr style='background:#f2f2f2'>
                        <th>Category</th>
                        <th>Budget</th>
                        <th>Spent</th>
                        <th>Remaining</th>
                        <th>Utilization</th>
                    </tr>
            """);

        for (BudgetAlertDTO alert : alerts) {
            String color = "CRITICAL".equals(alert.getAlertLevel()) ? "#FF0000" : "#FF9800";
            sb.append("<tr>")
                    .append("<td>").append(alert.getCategory()).append("</td>")
                    .append("<td>₹").append(alert.getBudgetAmount()).append("</td>")
                    .append("<td>₹").append(alert.getSpentAmount()).append("</td>")
                    .append("<td>₹").append(alert.getRemaining()).append("</td>")
                    .append("<td style='color:").append(color).append("'>")
                    .append(alert.getUtilizationPercentage()).append("% (")
                    .append(alert.getAlertLevel()).append(")</td>")
                    .append("</tr>");
        }

        sb.append("</table><br><p>Consider reviewing your spending habits.</p></div>");
        return sb.toString();
    }
}
