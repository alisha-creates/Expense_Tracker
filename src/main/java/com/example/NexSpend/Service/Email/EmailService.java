package com.example.NexSpend.Service.Email;

import com.example.NexSpend.Entity.RecurringExpense;

public interface EmailService {
    void sendActivationEmail(String toEmail,
                             String userName,
                             String activationToken);

    void sendHtmlEmail(
            String toEmail,
            String subject,
            String htmlContent);

    void sendEmailWithAttachment(
            String toEmail,
            String subject,
            String body,
            byte[] excelFile,
            byte[] pdfFile
    );

    void sendRecurringPaymentReminder(
            String toEmail,
            RecurringExpense recurring
    );

    void sendPasswordChangedEmail(String toEmail, String userName);
}
