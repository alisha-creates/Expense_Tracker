package com.example.NexSpend.Controller;

import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseRequestDTO;
import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseResponseDTO;
import com.example.NexSpend.Service.RecurringExpense.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringExpenseController {
    private final RecurringExpenseService recurringService;

    @PostMapping
    public ResponseEntity<RecurringExpenseResponseDTO> createRecurring(
            @Valid @RequestBody RecurringExpenseRequestDTO dto,
            Authentication authentication) {

        RecurringExpenseResponseDTO response =
                recurringService.createRecurring(dto, authentication);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponseDTO> updateRecurring(
            @PathVariable Long id,
            @Valid @RequestBody RecurringExpenseRequestDTO dto,
            Authentication authentication) {

        RecurringExpenseResponseDTO response =
                recurringService.updateRecurring(
                        id,
                        dto,
                        authentication
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponseDTO>> getUserRecurring(
            Authentication authentication) {

        List<RecurringExpenseResponseDTO> response =
                recurringService.getUserRecurring(authentication);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurring(
            @PathVariable Long id,
            Authentication authentication) {

        recurringService.deleteRecurring(
                id,
                authentication
        );

        return ResponseEntity.noContent().build();
    }
}
