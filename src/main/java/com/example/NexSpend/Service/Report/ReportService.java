package com.example.NexSpend.Service.Report;

import com.example.NexSpend.Entity.User;

public interface ReportService {
    byte[] generateMonthlyExcelReport(User user);

    byte[] generateMonthlyPdfReport(User user);
}
