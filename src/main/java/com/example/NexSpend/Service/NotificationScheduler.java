package com.example.NexSpend.Service;

import com.example.NexSpend.Service.Notification.NotificationService;
import com.example.NexSpend.Service.RecurringExpense.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationScheduler {
    private final NotificationService notificationService;

    private final RecurringExpenseService recurringService;

    @Scheduled(cron = "0 30 23 L * ?", zone = "Asia/Kolkata")
    public void monthlyReport() {
        notificationService.sendMonthlyReportsToAllUsers();
    }

    @Scheduled(
            cron = "0 0 9 * * *",
            zone = "Asia/Kolkata"
    )
    public void sendBudgetAlerts() {
        notificationService.sendBudgetAlertsToAllUsers();
    }

    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Kolkata")
    public void processRecurringExpenses() {
        recurringService.processDueRecurringExpenses();
    }

    @Scheduled(
            cron = "0 0 9 * * *",
            zone = "Asia/Kolkata"
    )
    public void sendRecurringPaymentReminders() {
        notificationService.sendRecurringPaymentRemindersToAllUsers();
    }
}