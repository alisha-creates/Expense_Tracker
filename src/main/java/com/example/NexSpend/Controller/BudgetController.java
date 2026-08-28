package com.example.NexSpend.Controller;

import com.example.NexSpend.DTO.BudgetDTO.BudgetRequestDTO;
import com.example.NexSpend.DTO.BudgetDTO.BudgetResponseDTO;
import com.example.NexSpend.Service.Budget.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponseDTO> createOrUpdateBudget(
            @Valid @RequestBody BudgetRequestDTO requestDTO,
            Authentication authentication) {

        BudgetResponseDTO response = budgetService.createBudget(requestDTO, authentication);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponseDTO>> getUserBudgets(Authentication authentication) {
        List<BudgetResponseDTO> budgets = budgetService.getUserBudgets(authentication);
        return ResponseEntity.ok(budgets);
    }

    @GetMapping("/current-month")
    public ResponseEntity<List<BudgetResponseDTO>> getCurrentMonthBudgets(Authentication authentication) {
        List<BudgetResponseDTO> budgets = budgetService.getCurrentMonthBudgets(authentication);
        return ResponseEntity.ok(budgets);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponseDTO> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequestDTO requestDTO,
            Authentication authentication) {

        BudgetResponseDTO response =
                budgetService.updateBudget(
                        id,
                        requestDTO,
                        authentication
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            Authentication authentication) {

        budgetService.deleteBudget(
                id,
                authentication
        );

        return ResponseEntity.noContent().build();
    }

}
