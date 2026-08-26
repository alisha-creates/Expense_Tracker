package com.example.NexSpend.Controller;


import com.example.NexSpend.Service.Notification.NotificationService;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping("/monthly-report")
    public String testMonthlyReport(Authentication authentication) {
        notificationService.sendMonthlyReport(currentUser(authentication));
        return "Monthly report sent";
    }
}
