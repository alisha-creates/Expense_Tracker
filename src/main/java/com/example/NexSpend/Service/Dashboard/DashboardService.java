package com.example.NexSpend.Service.Dashboard;

import com.example.NexSpend.DTO.DashboardResponseDTO;
import org.springframework.security.core.Authentication;

public interface DashboardService {
    DashboardResponseDTO getDashboard(Authentication authentication);
}
