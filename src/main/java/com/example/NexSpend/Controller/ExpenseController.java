package com.example.NexSpend.Controller;

import com.example.NexSpend.DTO.ExpenseDTO.ExpenseRequestDTO;
import com.example.NexSpend.DTO.ExpenseDTO.ExpenseResponseDTO;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.ExpenseType;
import com.example.NexSpend.Service.Expense.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponseDTO> createExpense(
            @RequestBody ExpenseRequestDTO requestDTO ) {
        ExpenseResponseDTO response =
                expenseService.createExpense(requestDTO);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> getExpenseById(
            @PathVariable Long id ) {
        ExpenseResponseDTO response =
                expenseService.getExpenseById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseResponseDTO>> getAllExpenses(
            Pageable pageable
    ) {
        Page<ExpenseResponseDTO> response =
                expenseService.getAllExpenses(pageable);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> updateExpense(
            @PathVariable Long id,
            @RequestBody ExpenseRequestDTO requestDTO
    ) {
        ExpenseResponseDTO response =
                expenseService.updateExpense(id, requestDTO);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(
            @PathVariable Long id
    ) {
        expenseService.deleteExpense(id);

        return ResponseEntity.ok("Expense deleted successfully");
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<ExpenseResponseDTO>> filterExpenses(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) ExpenseType type,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<ExpenseResponseDTO> response =
                expenseService.filterExpenses(
                        category,
                        type,
                        startDate,
                        endDate,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}
