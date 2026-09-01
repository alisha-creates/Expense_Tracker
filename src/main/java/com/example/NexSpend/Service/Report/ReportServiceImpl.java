package com.example.NexSpend.Service.Report;

import com.example.NexSpend.Entity.Budget;
import com.example.NexSpend.Entity.Expense;
import com.example.NexSpend.Entity.ExpenseType;
import com.example.NexSpend.Entity.RecurringExpense;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Exception.ReportGenerationException;
import com.example.NexSpend.Repository.BudgetRepository;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.RecurringExpenseRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;

    // ================================================================
    // EXCEL REPORT
    // ================================================================

    @Override
    public byte[] generateMonthlyExcelReport(User user) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        List<Expense> transactions =
                expenseRepository.findByUserAndDateBetween(user, startOfMonth, now);

        List<Budget> budgets =
                budgetRepository.findByUserAndYear(user, now.getYear());

        List<RecurringExpense> recurringExpenses =
                recurringExpenseRepository.findByUserAndActiveTrue(user);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            // ---------------------------------------------------------
            // STYLES
            // ---------------------------------------------------------
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle sectionStyle = createExcelSectionStyle(workbook);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle normalStyle = createExcelNormalStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle percentageStyle = createPercentageStyle(workbook);
            CellStyle positiveStyle = createPositiveStyle(workbook);
            CellStyle negativeStyle = createNegativeStyle(workbook);

            // ---------------------------------------------------------
            // CALCULATIONS
            // ---------------------------------------------------------
            BigDecimal totalIncome = calculateIncome(transactions);
            BigDecimal totalExpense = calculateExpense(transactions);
            BigDecimal netBalance = totalIncome.subtract(totalExpense);

            long incomeCount = transactions.stream()
                    .filter(t -> t.getType() == ExpenseType.INCOME).count();
            long expenseCount = transactions.stream()
                    .filter(t -> t.getType() == ExpenseType.EXPENSE).count();

            BigDecimal averageExpense = expenseCount == 0
                    ? BigDecimal.ZERO
                    : totalExpense.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP);

            // Fraction (0-1), rendered by the "0.00%" cell format.
            BigDecimal savingsRate = totalIncome.signum() == 0
                    ? BigDecimal.ZERO
                    : netBalance.divide(totalIncome, 4, RoundingMode.HALF_UP);

            long recurringCount = countRecurringExpenseEntries(recurringExpenses);
            BigDecimal recurringTotal = calculateRecurringExpenseAmount(recurringExpenses);

            // ---------------------------------------------------------
            // CATEGORY DATA
            // ---------------------------------------------------------
            Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
            Map<String, Integer> categoryCounts = new LinkedHashMap<>();

            for (Expense transaction : transactions) {
                if (transaction.getType() != ExpenseType.EXPENSE) {
                    continue;
                }
                String category = transaction.getCategory() == null
                        ? "OTHER" : transaction.getCategory().name();
                categoryTotals.merge(category, transaction.getAmount(), BigDecimal::add);
                categoryCounts.merge(category, 1, Integer::sum);
            }

            List<Map.Entry<String, BigDecimal>> sortedCategories =
                    new ArrayList<>(categoryTotals.entrySet());
            sortedCategories.sort(Map.Entry.<String, BigDecimal>comparingByValue().reversed());

            // ---------------------------------------------------------
            // BUDGET CALCULATIONS
            // ---------------------------------------------------------
            BigDecimal totalBudget = BigDecimal.ZERO;
            BigDecimal totalSpent = BigDecimal.ZERO;
            int overBudgetCount = 0;
            Map<Long, BigDecimal> budgetSpent = new LinkedHashMap<>();

            for (Budget budget : budgets) {
                BigDecimal spent = expenseRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                        user.getId(), budget.getCategory(), budget.getMonth(), budget.getYear());
                if (spent == null) {
                    spent = BigDecimal.ZERO;
                }
                budgetSpent.put(budget.getId(), spent);
                totalBudget = totalBudget.add(budget.getAmount());
                totalSpent = totalSpent.add(spent);
                if (spent.compareTo(budget.getAmount()) > 0) {
                    overBudgetCount++;
                }
            }

            BigDecimal totalRemaining = totalBudget.subtract(totalSpent);

            // Fraction (0-1), rendered by the "0.00%" cell format.
            BigDecimal totalUtilization = totalBudget.signum() == 0
                    ? BigDecimal.ZERO
                    : totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP);

            // ---------------------------------------------------------
            // SHEET 1 - DASHBOARD
            // ---------------------------------------------------------
            Sheet dashboard = workbook.createSheet("Dashboard");
            dashboard.setDisplayGridlines(false);
            mergeCells(dashboard, 0, 0, 0, 7);

            addExcelRow(dashboard, 0, titleStyle, "NEXSPEND - MONTHLY FINANCIAL REPORT");

            addExcelRow(dashboard, 2, sectionStyle, "Report Information");
            addExcelRow(dashboard, 3, normalStyle, "Report Period",
                    startOfMonth.toLocalDate() + " to " + now.toLocalDate());
            addExcelRow(dashboard, 4, normalStyle, "Generated At", now);
            dashboard.getRow(4).getCell(1).setCellStyle(dateStyle);
            addExcelRow(dashboard, 6, normalStyle, "User Name", user.getName());
            addExcelRow(dashboard, 7, normalStyle, "Email", user.getEmail());

            // KPI SECTION
            addExcelRow(dashboard, 9, sectionStyle, "Financial Overview");
            addExcelRow(dashboard, 10, headerStyle, "Metric", "Value");

            addExcelRow(dashboard, 11, normalStyle, "Total Income", totalIncome);
            dashboard.getRow(11).getCell(1).setCellStyle(currencyStyle);

            addExcelRow(dashboard, 12, normalStyle, "Total Expense", totalExpense);
            dashboard.getRow(12).getCell(1).setCellStyle(currencyStyle);

            addExcelRow(dashboard, 13, normalStyle, "Net Balance", netBalance);
            dashboard.getRow(13).getCell(1)
                    .setCellStyle(netBalance.signum() >= 0 ? positiveStyle : negativeStyle);

            addExcelRow(dashboard, 14, normalStyle, "Transaction Count", transactions.size());
            addExcelRow(dashboard, 15, normalStyle, "Income Transactions", incomeCount);
            addExcelRow(dashboard, 16, normalStyle, "Expense Transactions", expenseCount);

            addExcelRow(dashboard, 17, normalStyle, "Average Expense", averageExpense);
            dashboard.getRow(17).getCell(1).setCellStyle(currencyStyle);

            addExcelRow(dashboard, 18, normalStyle, "Savings Rate", savingsRate);
            dashboard.getRow(18).getCell(1).setCellStyle(percentageStyle);

            addExcelRow(dashboard, 19, normalStyle, "Recurring Expense Count", recurringCount);

            addExcelRow(dashboard, 20, normalStyle, "Recurring Expense Total", recurringTotal);
            dashboard.getRow(20).getCell(1).setCellStyle(currencyStyle);

            // CATEGORY ANALYSIS
            addExcelRow(dashboard, 22, sectionStyle, "Category-wise Expense Analysis");
            addExcelRow(dashboard, 23, headerStyle, "Category", "Amount", "Percentage", "Transactions");

            int categoryRow = 24;
            for (Map.Entry<String, BigDecimal> entry : sortedCategories) {
                BigDecimal percentage = totalExpense.signum() == 0
                        ? BigDecimal.ZERO
                        : entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP);

                addExcelRow(dashboard, categoryRow, normalStyle,
                        entry.getKey(), entry.getValue(), percentage, categoryCounts.get(entry.getKey()));

                dashboard.getRow(categoryRow).getCell(1).setCellStyle(currencyStyle);
                dashboard.getRow(categoryRow).getCell(2).setCellStyle(percentageStyle);
                categoryRow++;
            }

            // BUDGET SUMMARY (on dashboard)
            int budgetStart = categoryRow + 2;
            addExcelRow(dashboard, budgetStart, sectionStyle, "Budget Overview");
            addExcelRow(dashboard, budgetStart + 1, headerStyle, "Metric", "Value");

            addExcelRow(dashboard, budgetStart + 2, normalStyle, "Total Budget", totalBudget);
            dashboard.getRow(budgetStart + 2).getCell(1).setCellStyle(currencyStyle);

            addExcelRow(dashboard, budgetStart + 3, normalStyle, "Total Spent", totalSpent);
            dashboard.getRow(budgetStart + 3).getCell(1).setCellStyle(currencyStyle);

            addExcelRow(dashboard, budgetStart + 4, normalStyle, "Remaining", totalRemaining);
            dashboard.getRow(budgetStart + 4).getCell(1)
                    .setCellStyle(totalRemaining.signum() >= 0 ? positiveStyle : negativeStyle);

            addExcelRow(dashboard, budgetStart + 5, normalStyle, "Utilization", totalUtilization);
            dashboard.getRow(budgetStart + 5).getCell(1).setCellStyle(percentageStyle);

            addExcelRow(dashboard, budgetStart + 6, normalStyle, "Over Budget Count", overBudgetCount);

            setExcelWidths(dashboard, 7500, 6000, 4500, 4000, 4000, 4000, 4000, 4000);

            // ---------------------------------------------------------
            // SHEET 2 - TRANSACTIONS
            // ---------------------------------------------------------
            Sheet transactionSheet = workbook.createSheet("Transactions");
            transactionSheet.setDisplayGridlines(false);

            addExcelRow(transactionSheet, 0, titleStyle, "NEXSPEND TRANSACTION DETAILS");
            mergeCells(transactionSheet, 0, 0, 0, 6);

            addExcelRow(transactionSheet, 2, headerStyle,
                    "#", "Description", "Amount", "Category", "Type",
                    "Transaction Date", "Updated At");

            int rowIndex = 3;
            int transactionDisplayId = 1;
            for (Expense transaction : transactions) {
                addExcelRow(transactionSheet, rowIndex, normalStyle,
                        transactionDisplayId,
                        transaction.getDescription() == null ? "" : transaction.getDescription(),
                        transaction.getAmount(),
                        transaction.getCategory() == null ? "" : transaction.getCategory().name(),
                        transaction.getType() == null ? "" : transaction.getType().name(),
                        transaction.getDate(),
                        transaction.getUpdatedAt());

                transactionSheet.getRow(rowIndex).getCell(2).setCellStyle(currencyStyle);
                for (int column : List.of(5, 6)) {
                    transactionSheet.getRow(rowIndex).getCell(column).setCellStyle(dateStyle);
                }
                rowIndex++;
                transactionDisplayId++;
            }

            transactionSheet.createFreezePane(0, 3);
            setExcelWidths(transactionSheet, 3000, 11000, 5000, 5000, 4000, 6500, 6500);

            // ---------------------------------------------------------
            // SHEET 3 - BUDGETS
            // ---------------------------------------------------------
            Sheet budgetSheet = workbook.createSheet("Budgets");
            budgetSheet.setDisplayGridlines(false);

            addExcelRow(budgetSheet, 0, titleStyle, "NEXSPEND BUDGET ANALYSIS");
            mergeCells(budgetSheet, 0, 0, 0, 7);

            addExcelRow(budgetSheet, 2, headerStyle,
                    "#", "Category", "Budget Amount", "Month", "Year",
                    "Remaining", "Utilization %", "Updated At");

            int budgetRow = 3;
            int budgetDisplayId = 1;
            for (Budget budget : budgets) {
                BigDecimal spent = budgetSpent.getOrDefault(budget.getId(), BigDecimal.ZERO);
                BigDecimal remaining = budget.getAmount().subtract(spent);
                BigDecimal utilization = budget.getAmount().signum() == 0
                        ? BigDecimal.ZERO
                        : spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP);

                addExcelRow(budgetSheet, budgetRow, normalStyle,
                        budgetDisplayId,
                        budget.getCategory() == null ? "" : budget.getCategory().name(),
                        budget.getAmount(),
                        budget.getMonth(),
                        budget.getYear(),
                        remaining,
                        utilization,
                        budget.getUpdatedAt());

                budgetSheet.getRow(budgetRow).getCell(2).setCellStyle(currencyStyle);
                budgetSheet.getRow(budgetRow).getCell(5)
                        .setCellStyle(remaining.signum() >= 0 ? positiveStyle : negativeStyle);
                budgetSheet.getRow(budgetRow).getCell(6).setCellStyle(percentageStyle);
                budgetSheet.getRow(budgetRow).getCell(7).setCellStyle(dateStyle);

                budgetRow++;
                budgetDisplayId++;
            }

            addExcelRow(budgetSheet, budgetRow + 1, headerStyle,
                    "TOTAL", "", totalBudget, "", "", totalRemaining, totalUtilization, "");

            budgetSheet.getRow(budgetRow + 1).getCell(2).setCellStyle(currencyStyle);
            budgetSheet.getRow(budgetRow + 1).getCell(5)
                    .setCellStyle(totalRemaining.signum() >= 0 ? positiveStyle : negativeStyle);
            budgetSheet.getRow(budgetRow + 1).getCell(6).setCellStyle(percentageStyle);

            budgetSheet.createFreezePane(0, 3);
            setExcelWidths(budgetSheet, 3000, 5500, 5500, 3500, 3500, 5500, 4800, 6500);

            // ---------------------------------------------------------
            // SHEET 4 - CATEGORY ANALYSIS
            // ---------------------------------------------------------
            Sheet categorySheet = workbook.createSheet("Category Analysis");
            categorySheet.setDisplayGridlines(false);

            addExcelRow(categorySheet, 0, titleStyle, "NEXSPEND CATEGORY ANALYSIS");
            mergeCells(categorySheet, 0, 0, 0, 4);

            addExcelRow(categorySheet, 2, headerStyle,
                    "Category", "Expense Amount", "Percentage", "Transaction Count");

            int categorySheetRow = 3;
            for (Map.Entry<String, BigDecimal> entry : sortedCategories) {
                BigDecimal percentage = totalExpense.signum() == 0
                        ? BigDecimal.ZERO
                        : entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP);

                addExcelRow(categorySheet, categorySheetRow, normalStyle,
                        entry.getKey(), entry.getValue(), percentage, categoryCounts.get(entry.getKey()));

                categorySheet.getRow(categorySheetRow).getCell(1).setCellStyle(currencyStyle);
                categorySheet.getRow(categorySheetRow).getCell(2).setCellStyle(percentageStyle);
                categorySheetRow++;
            }

            setExcelWidths(categorySheet, 5000, 6000, 5000, 5000);

            // ---------------------------------------------------------
            // SHEET 5 - RECURRING EXPENSES
            // ---------------------------------------------------------
            Sheet recurringSheet = workbook.createSheet("Recurring Expenses");
            recurringSheet.setDisplayGridlines(false);

            addExcelRow(recurringSheet, 0, titleStyle, "NEXSPEND RECURRING EXPENSES");
            mergeCells(recurringSheet, 0, 0, 0, 6);

            addExcelRow(recurringSheet, 2, headerStyle,
                    "#", "Description", "Amount", "Category", "Type", "Frequency", "Next Execution Date");

            int recurringRow = 3;
            int recurringDisplayId = 1;
            for (RecurringExpense recurring : recurringExpenses) {
                addExcelRow(recurringSheet, recurringRow, normalStyle,
                        recurringDisplayId,
                        recurring.getDescription() == null ? "" : recurring.getDescription(),
                        recurring.getAmount(),
                        recurring.getCategory() == null ? "" : recurring.getCategory().name(),
                        recurring.getType() == null ? "" : recurring.getType().name(),
                        recurring.getFrequency() == null ? "" : recurring.getFrequency().name(),
                        recurring.getNextExecutionDate());

                recurringSheet.getRow(recurringRow).getCell(2).setCellStyle(currencyStyle);
                recurringSheet.getRow(recurringRow).getCell(6).setCellStyle(dateStyle);

                recurringRow++;
                recurringDisplayId++;
            }

            if (recurringExpenses.isEmpty()) {
                addExcelRow(recurringSheet, recurringRow, normalStyle, "No active recurring expenses");
                recurringRow++;
            }

            addExcelRow(recurringSheet, recurringRow + 1, headerStyle,
                    "TOTAL", "", recurringTotal, "", "", "", "");
            recurringSheet.getRow(recurringRow + 1).getCell(2).setCellStyle(currencyStyle);

            recurringSheet.createFreezePane(0, 3);
            setExcelWidths(recurringSheet, 3000, 11000, 5000, 5000, 4000, 4000, 6500);

            // ---------------------------------------------------------
            // SHEET 6 - BUDGET SUMMARY
            // ---------------------------------------------------------
            Sheet budgetSummary = workbook.createSheet("Budget Summary");
            budgetSummary.setDisplayGridlines(false);

            addExcelRow(budgetSummary, 0, titleStyle, "NEXSPEND BUDGET SUMMARY");
            mergeCells(budgetSummary, 0, 0, 0, 3);

            addExcelRow(budgetSummary, 2, headerStyle, "Metric", "Value");
            addExcelRow(budgetSummary, 3, normalStyle, "Total Budget", totalBudget);
            addExcelRow(budgetSummary, 4, normalStyle, "Total Spent", totalSpent);
            addExcelRow(budgetSummary, 5, normalStyle, "Remaining", totalRemaining);
            addExcelRow(budgetSummary, 6, normalStyle, "Utilization", totalUtilization);
            addExcelRow(budgetSummary, 7, normalStyle, "Over Budget Count", overBudgetCount);

            for (int row : List.of(3, 4, 5)) {
                budgetSummary.getRow(row).getCell(1).setCellStyle(currencyStyle);
            }
            budgetSummary.getRow(6).getCell(1).setCellStyle(percentageStyle);

            setExcelWidths(budgetSummary, 7000, 6000);

            // ---------------------------------------------------------
            // WRITE FILE
            // ---------------------------------------------------------
            workbook.write(output);
            return output.toByteArray();

        } catch (Exception exception) {
            throw new ReportGenerationException(
                    "Error generating Excel report: " + exception.getMessage());
        }
    }

    // ================================================================
    // PDF REPORT
    // ================================================================

    @Override
    public byte[] generateMonthlyPdfReport(User user) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        List<Expense> transactions =
                expenseRepository.findByUserAndDateBetween(user, startOfMonth, now);

        List<Budget> budgets =
                budgetRepository.findByUserAndYear(user, now.getYear());

        List<RecurringExpense> recurringExpenses =
                recurringExpenseRepository.findByUserAndActiveTrue(user);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PdfPageEvent());
            document.open();

            // ---------------------------------------------------------
            // FONTS
            // ---------------------------------------------------------
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // ---------------------------------------------------------
            // CALCULATIONS
            // ---------------------------------------------------------
            BigDecimal totalIncome = calculateIncome(transactions);
            BigDecimal totalExpense = calculateExpense(transactions);
            BigDecimal balance = totalIncome.subtract(totalExpense);

            long incomeCount = transactions.stream()
                    .filter(t -> t.getType() == ExpenseType.INCOME).count();
            long expenseCount = transactions.stream()
                    .filter(t -> t.getType() == ExpenseType.EXPENSE).count();

            BigDecimal averageExpense = expenseCount == 0
                    ? BigDecimal.ZERO
                    : totalExpense.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP);

            BigDecimal savingsRate = totalIncome.signum() == 0
                    ? BigDecimal.ZERO
                    : balance.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);

            long recurringCount = countRecurringExpenseEntries(recurringExpenses);
            BigDecimal recurringTotal = calculateRecurringExpenseAmount(recurringExpenses);

            // ---------------------------------------------------------
            // CATEGORY
            // ---------------------------------------------------------
            Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
            Map<String, Integer> categoryCounts = new LinkedHashMap<>();

            for (Expense transaction : transactions) {
                if (transaction.getType() != ExpenseType.EXPENSE) {
                    continue;
                }
                String category = transaction.getCategory() == null
                        ? "OTHER" : transaction.getCategory().name();
                categoryTotals.merge(category, transaction.getAmount(), BigDecimal::add);
                categoryCounts.merge(category, 1, Integer::sum);
            }

            List<Map.Entry<String, BigDecimal>> sortedCategories =
                    new ArrayList<>(categoryTotals.entrySet());
            sortedCategories.sort(Map.Entry.<String, BigDecimal>comparingByValue().reversed());

            // ---------------------------------------------------------
            // TITLE
            // ---------------------------------------------------------
            Paragraph title = new Paragraph("NEXSPEND", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "Personal Finance & Expense Management Report", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // USER INFO
            // ---------------------------------------------------------
            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            info.setSpacingAfter(10f);

            addInfoCell(info, "REPORT PERIOD", true);
            addInfoCell(info, startOfMonth.toLocalDate() + " to " + now.toLocalDate(), false);
            addInfoCell(info, "GENERATED", true);
            addInfoCell(info, now.toString(), false);
            addInfoCell(info, "USER", true);
            addInfoCell(info, user.getName(), false);
            addInfoCell(info, "EMAIL", true);
            addInfoCell(info, user.getEmail(), false);

            document.add(info);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // FINANCIAL OVERVIEW
            // ---------------------------------------------------------
            document.add(new Paragraph("Financial Overview", sectionFont));

            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setSpacingBefore(4f);
            summary.setSpacingAfter(10f);

            addSummaryCell(summary, "TOTAL INCOME", formatCurrency(totalIncome));
            addSummaryCell(summary, "TOTAL EXPENSE", formatCurrency(totalExpense));
            addSummaryCell(summary, "NET BALANCE", formatCurrency(balance));
            addSummaryCell(summary, "TRANSACTIONS", String.valueOf(transactions.size()));

            document.add(summary);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // STATISTICS
            // ---------------------------------------------------------
            PdfPTable stats = new PdfPTable(5);
            stats.setWidthPercentage(100);
            stats.setSpacingBefore(4f);
            stats.setSpacingAfter(10f);

            addStatisticCell(stats, "Income Transactions", String.valueOf(incomeCount));
            addStatisticCell(stats, "Expense Transactions", String.valueOf(expenseCount));
            addStatisticCell(stats, "Average Expense", formatCurrency(averageExpense));
            addStatisticCell(stats, "Savings Rate", savingsRate + "%");
            addStatisticCell(stats, "Recurring Expenses",
                    recurringCount + " tx \u2022 " + formatCurrency(recurringTotal));

            document.add(stats);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // CATEGORY ANALYSIS
            // ---------------------------------------------------------
            document.add(new Paragraph("Category-wise Expense Analysis", sectionFont));

            PdfPTable categoryTable = new PdfPTable(4);
            categoryTable.setWidthPercentage(100);
            categoryTable.setSpacingBefore(4f);
            categoryTable.setSpacingAfter(10f);
            addTableHeader(categoryTable, "Category", "Amount", "% of Expenses", "Transactions");

            for (Map.Entry<String, BigDecimal> entry : sortedCategories) {
                BigDecimal percentage = totalExpense.signum() == 0
                        ? BigDecimal.ZERO
                        : entry.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP);

                categoryTable.addCell(createCell(entry.getKey(), normalFont));
                categoryTable.addCell(createCell(formatCurrency(entry.getValue()), normalFont));
                categoryTable.addCell(createCell(percentage + "%", normalFont));
                categoryTable.addCell(createCell(
                        String.valueOf(categoryCounts.get(entry.getKey())), normalFont));
            }

            if (categoryTotals.isEmpty()) {
                PdfPCell empty = createCell("No expenses recorded", normalFont);
                empty.setColspan(4);
                categoryTable.addCell(empty);
            }

            document.add(categoryTable);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // RECURRING EXPENSES
            // ---------------------------------------------------------
            document.add(new Paragraph("Recurring Expenses", sectionFont));

            PdfPTable recurringTable = new PdfPTable(6);
            recurringTable.setWidthPercentage(100);
            recurringTable.setSpacingBefore(4f);
            recurringTable.setSpacingAfter(10f);
            addTableHeader(recurringTable,
                    "Description", "Amount", "Category", "Type", "Frequency", "Next Execution Date");

            for (RecurringExpense recurring : recurringExpenses) {
                recurringTable.addCell(createCell(
                        recurring.getDescription() == null ? "" : recurring.getDescription(), normalFont));
                recurringTable.addCell(createCell(formatCurrency(recurring.getAmount()), normalFont));
                recurringTable.addCell(createCell(
                        recurring.getCategory() == null ? "" : recurring.getCategory().name(), normalFont));
                recurringTable.addCell(createCell(
                        recurring.getType() == null ? "" : recurring.getType().name(), normalFont));
                recurringTable.addCell(createCell(
                        recurring.getFrequency() == null ? "" : recurring.getFrequency().name(), normalFont));
                recurringTable.addCell(createCell(
                        String.valueOf(recurring.getNextExecutionDate()), normalFont));
            }

            if (recurringExpenses.isEmpty()) {
                PdfPCell emptyRecurring = createCell("No active recurring expenses", normalFont);
                emptyRecurring.setColspan(6);
                recurringTable.addCell(emptyRecurring);
            }

            document.add(recurringTable);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // TRANSACTIONS
            // ---------------------------------------------------------
            document.add(new Paragraph("Transaction Details", sectionFont));

            PdfPTable transactionTable = new PdfPTable(7);
            transactionTable.setWidthPercentage(100);
            transactionTable.setSpacingBefore(4f);
            transactionTable.setSpacingAfter(10f);
            transactionTable.setWidths(new float[]{
                    .5f, 2.6f, 1.3f, 1.4f, 1.1f, 1.8f, 1.8f
            });

            addTableHeader(transactionTable,
                    "#", "Description", "Amount", "Category", "Type",
                    "Transaction Date", "Updated At");

            int transactionDisplayId = 1;
            for (Expense transaction : transactions) {
                transactionTable.addCell(createCell(String.valueOf(transactionDisplayId), smallFont));
                transactionTable.addCell(createCell(
                        transaction.getDescription() == null ? "" : transaction.getDescription(), smallFont));
                transactionTable.addCell(createCell(formatCurrency(transaction.getAmount()), smallFont));
                transactionTable.addCell(createCell(
                        transaction.getCategory() == null ? "" : transaction.getCategory().name(), smallFont));
                transactionTable.addCell(createCell(
                        transaction.getType() == null ? "" : transaction.getType().name(), smallFont));
                transactionTable.addCell(createCell(String.valueOf(transaction.getDate()), smallFont));
                transactionTable.addCell(createCell(String.valueOf(transaction.getUpdatedAt()), smallFont));
                transactionDisplayId++;
            }

            document.add(transactionTable);

            // ---------------------------------------------------------
            // NEW PAGE FOR BUDGET
            // ---------------------------------------------------------
            document.newPage();

            document.add(new Paragraph("Budget Analysis", titleFont));
            document.add(new Paragraph("Budget performance for " + now.getYear(), subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable budgetTable = new PdfPTable(8);
            budgetTable.setWidthPercentage(100);
            budgetTable.setSpacingBefore(4f);
            budgetTable.setSpacingAfter(10f);
            budgetTable.setWidths(new float[]{
                    .6f, 1.7f, 1.6f, .9f, .9f, 1.6f, 1.4f, 1.8f
            });

            addTableHeader(budgetTable,
                    "#", "Category", "Budget Amount", "Month", "Year",
                    "Remaining", "Utilization", "Updated At");

            BigDecimal totalBudget = BigDecimal.ZERO;
            BigDecimal totalSpent = BigDecimal.ZERO;
            int overBudgetCount = 0;
            int budgetDisplayId = 1;

            for (Budget budget : budgets) {
                BigDecimal spent = expenseRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                        user.getId(), budget.getCategory(), budget.getMonth(), budget.getYear());
                if (spent == null) {
                    spent = BigDecimal.ZERO;
                }

                BigDecimal remaining = budget.getAmount().subtract(spent);
                BigDecimal utilization = budget.getAmount().signum() == 0
                        ? BigDecimal.ZERO
                        : spent.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);

                if (spent.compareTo(budget.getAmount()) > 0) {
                    overBudgetCount++;
                }

                totalBudget = totalBudget.add(budget.getAmount());
                totalSpent = totalSpent.add(spent);

                addBudgetRow(budgetTable, budgetDisplayId, budget, remaining, utilization, smallFont);
                budgetDisplayId++;
            }

            BigDecimal totalRemaining = totalBudget.subtract(totalSpent);
            BigDecimal totalUtilization = totalBudget.signum() == 0
                    ? BigDecimal.ZERO
                    : totalSpent.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 2, RoundingMode.HALF_UP);

            addTableHeader(budgetTable,
                    "TOTAL", "", formatCurrency(totalBudget), "", "",
                    formatCurrency(totalRemaining), totalUtilization + "%", "");

            document.add(budgetTable);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // BUDGET SUMMARY
            // ---------------------------------------------------------
            document.add(new Paragraph("Budget Summary", sectionFont));

            PdfPTable budgetSummary = new PdfPTable(4);
            budgetSummary.setWidthPercentage(100);
            budgetSummary.setSpacingBefore(4f);
            budgetSummary.setSpacingAfter(10f);

            addSummaryCell(budgetSummary, "TOTAL BUDGET", formatCurrency(totalBudget));
            addSummaryCell(budgetSummary, "TOTAL SPENT", formatCurrency(totalSpent));
            addSummaryCell(budgetSummary, "REMAINING", formatCurrency(totalRemaining));
            addSummaryCell(budgetSummary, "OVER BUDGET", String.valueOf(overBudgetCount));

            document.add(budgetSummary);
            document.add(new Paragraph(" "));

            // ---------------------------------------------------------
            // FOOTNOTE
            // ---------------------------------------------------------
            Paragraph note = new Paragraph(
                    "This report was automatically generated by NexSpend. All monetary values are "
                            + "displayed in Indian Rupees (\u20B9). Transaction data represents the selected "
                            + "monthly reporting period. Budget data represents the user's " + now.getYear()
                            + " budgets.",
                    smallFont);

            document.add(note);
            document.close();

            return output.toByteArray();

        } catch (Exception exception) {
            throw new ReportGenerationException(
                    "Failed to generate PDF report: " + exception.getMessage());
        }
    }

    // ================================================================
    // SHARED CALCULATIONS
    // ================================================================

    private BigDecimal calculateIncome(List<Expense> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == ExpenseType.INCOME)
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateExpense(List<Expense> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == ExpenseType.EXPENSE)
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return "\u20B9" + amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Counts active recurring expense definitions (type EXPENSE) for the user, sourced from the
     * real RecurringExpense entity/repository rather than being guessed from a description string.
     */
    private long countRecurringExpenseEntries(List<RecurringExpense> recurringExpenses) {
        return recurringExpenses.stream()
                .filter(r -> r.getType() == ExpenseType.EXPENSE)
                .count();
    }

    /**
     * Sums the amount of active recurring expense definitions (type EXPENSE) for the user, sourced
     * from the real RecurringExpense entity/repository.
     */
    private BigDecimal calculateRecurringExpenseAmount(List<RecurringExpense> recurringExpenses) {
        return recurringExpenses.stream()
                .filter(r -> r.getType() == ExpenseType.EXPENSE)
                .map(RecurringExpense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ================================================================
    // EXCEL HELPERS
    // ================================================================

    private void addExcelRow(Sheet sheet, int rowNumber, CellStyle style, Object... values) {
        Row row = sheet.getRow(rowNumber);
        if (row == null) {
            row = sheet.createRow(rowNumber);
        }

        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object value = values[i];

            if (value instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) value).doubleValue());
            } else if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else if (value instanceof LocalDateTime) {
                cell.setCellValue((LocalDateTime) value);
            } else {
                cell.setCellValue(value == null ? "" : value.toString());
            }

            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private CellStyle createExcelTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle createExcelSectionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createExcelHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createExcelNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createExcelNormalStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("\"\u20B9\"#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createExcelNormalStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        return style;
    }

    private CellStyle createPercentageStyle(Workbook workbook) {
        CellStyle style = createExcelNormalStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private CellStyle createPositiveStyle(Workbook workbook) {
        return createCurrencyStyle(workbook);
    }

    private CellStyle createNegativeStyle(Workbook workbook) {
        return createCurrencyStyle(workbook);
    }

    private void setExcelWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    /**
     * Apache POI's Sheet interface has no mergeCells(...) method — merging is done via
     * addMergedRegion(CellRangeAddress). This wraps that so call sites stay simple.
     */
    private void mergeCells(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    // ================================================================
    // PDF HELPERS
    // ================================================================

    private void addInfoCell(PdfPTable table, String text, boolean label) {
        Font font = FontFactory.getFont(
                FontFactory.HELVETICA, label ? 8 : 9, label ? Font.BOLD : Font.NORMAL);

        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text, font));
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOX);

        table.addCell(cell);
    }

    private void addSummaryCell(PdfPTable table, String title, String value) {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);

        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);

        Paragraph valueParagraph = new Paragraph(value, valueFont);
        valueParagraph.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(titleParagraph);
        cell.addElement(valueParagraph);

        table.addCell(cell);
    }

    private void addStatisticCell(PdfPTable table, String title, String value) {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);

        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);

        Paragraph valueParagraph = new Paragraph(value, valueFont);
        valueParagraph.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(titleParagraph);
        cell.addElement(valueParagraph);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(7);
            cell.setPaddingLeft(8);
            cell.setPaddingRight(8);
            cell.setBackgroundColor(new BaseColor(220, 230, 240));
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text, font));
        cell.setPadding(6);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void addBudgetRow(PdfPTable table, int displayId, Budget budget,
                              BigDecimal remaining, BigDecimal utilization, Font font) {

        table.addCell(createCell(String.valueOf(displayId), font));
        table.addCell(createCell(
                budget.getCategory() == null ? "" : budget.getCategory().name(), font));
        table.addCell(createCell(formatCurrency(budget.getAmount()), font));
        table.addCell(createCell(String.valueOf(budget.getMonth()), font));
        table.addCell(createCell(String.valueOf(budget.getYear()), font));
        table.addCell(createCell(formatCurrency(remaining), font));
        table.addCell(createCell(utilization + "%", font));
        table.addCell(createCell(String.valueOf(budget.getUpdatedAt()), font));
    }

    // ================================================================
    // PDF PAGE FOOTER
    // ================================================================

    public static class PdfPageEvent extends PdfPageEventHelper {

        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase footer = new Phrase(
                    "NexSpend \u2022 Personal Finance Report     |     Page " + writer.getPageNumber(),
                    footerFont);

            float x = (document.left() + document.right()) / 2;
            float y = document.bottom() - 20;

            writer.getDirectContent().beginText();
            writer.getDirectContent().setFontAndSize(footerFont.getBaseFont(), 8);
            writer.getDirectContent().showTextAligned(
                    Element.ALIGN_CENTER, footer.getContent(), x, y, 0);
            writer.getDirectContent().endText();
        }
    }
}