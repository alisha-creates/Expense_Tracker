package com.example.NexSpend.Service.Notification;

import com.example.NexSpend.Entity.User;

public interface NotificationService {
    void sendMonthlyReport(User user);

    void sendMonthlyReportsToAllUsers();

    void sendBudgetAlerts(User user);

    void sendBudgetAlertsToAllUsers();

    void sendRecurringPaymentReminders(User user);

    void sendRecurringPaymentRemindersToAllUsers();
}
