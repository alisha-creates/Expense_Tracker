package com.example.NexSpend.Service.Dashboard;

import com.example.NexSpend.DTO.DashboardResponseDTO;
import com.example.NexSpend.DTO.TrendDTO.TrendResponseDTO;
import org.springframework.security.core.Authentication;

public interface DashboardService {
    DashboardResponseDTO getDashboard(Authentication authentication);

    TrendResponseDTO getWeeklyTrend(int weekOffset, Authentication authentication);
}
