package com.example.NexSpend.Controller;

import com.example.NexSpend.DTO.DashboardResponseDTO;
import com.example.NexSpend.DTO.TrendDTO.TrendResponseDTO;
import com.example.NexSpend.Service.Dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getDashboard(authentication));
    }

    @GetMapping("/trend")
    public ResponseEntity<TrendResponseDTO> getWeeklyTrend(
            @RequestParam(defaultValue = "0") int weekOffset,
            Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getWeeklyTrend(weekOffset, authentication));
    }
}
