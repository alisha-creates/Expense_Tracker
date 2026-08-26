package com.example.NexSpend.Service.Notification;

import com.example.NexSpend.DTO.BudgetDTO.BudgetAlertDTO;
import com.example.NexSpend.Entity.Expense;
import com.example.NexSpend.Entity.ExpenseType;
import com.example.NexSpend.Entity.RecurringExpense;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.RecurringExpenseRepository;
import com.example.NexSpend.Repository.UserRepository;
import com.example.NexSpend.Service.Budget.BudgetService;
import com.example.NexSpend.Service.Email.EmailService;
import com.example.NexSpend.Service.Report.ReportService;
import com.example.NexSpend.Service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailService emailService;
    private final ReportService excelService;
    private final TemplateService templateService;
    private final BudgetService budgetService;
    private final RecurringExpenseRepository recurringExpenseRepository;


    @Override
    @Transactional(readOnly = true)
    public void sendMonthlyReport(User user) {
        byte[] excelFile = excelService.generateMonthlyExcelReport(user);

        byte[] pdfFile = excelService.generateMonthlyPdfReport(user);

        emailService.sendEmailWithAttachment(user.getEmail(), "NexSpend Monthly Reports",
                "Your monthly Excel and PDF reports are attached.", excelFile, pdfFile);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendBudgetAlerts(User user) {

        List<BudgetAlertDTO> alerts =
                budgetService.checkBudgetAlerts(user);

        if (alerts.isEmpty()) {
            return;
        }

        String alertHtml =
                templateService.buildBudgetAlert(alerts);

        emailService.sendHtmlEmail(
                user.getEmail(),
                "⚠️ Budget Alert - NexSpend",
                alertHtml
        );
    }

    @Override
    public void sendBudgetAlertsToAllUsers() {

        userRepository
                .findAll()
                .forEach(this::sendBudgetAlerts);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendRecurringPaymentReminders(User user) {

        LocalDateTime tomorrowStart =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();

        LocalDateTime tomorrowEnd =
                tomorrowStart
                        .plusDays(1)
                        .minusNanos(1);

        List<RecurringExpense> recurringExpenses =
                recurringExpenseRepository
                        .findByUserAndActiveTrueAndNextExecutionDateBetween(
                                user,
                                tomorrowStart,
                                tomorrowEnd
                        );

        for (RecurringExpense recurring : recurringExpenses) {

            emailService.sendRecurringPaymentReminder(
                    user.getEmail(),
                    recurring
            );
        }
    }

    @Override
    public void sendMonthlyReportsToAllUsers() {
        userRepository.findAll().forEach(this::sendMonthlyReport);
    }

    @Override
    public void sendRecurringPaymentRemindersToAllUsers() {

        userRepository
                .findAll()
                .forEach(this::sendRecurringPaymentReminders);
    }
}
