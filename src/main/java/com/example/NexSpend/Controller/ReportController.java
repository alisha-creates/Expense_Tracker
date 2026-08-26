package com.example.NexSpend.Controller;

import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Repository.UserRepository;
import com.example.NexSpend.Service.Report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    private final UserRepository userRepository;

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        byte[] file =
                reportService.generateMonthlyExcelReport(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=nexspend-report.xlsx"
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(file);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        byte[] file =
                reportService.generateMonthlyPdfReport(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=nexspend-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
}
