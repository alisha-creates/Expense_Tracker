package com.example.expensetracker.controller;

import com.example.expensetracker.dto.*;
import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.service.Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/expenses")

public class Controller {
    @Autowired
    Service service;

    @PostMapping
    public ResponseDTO createExpense(@Valid @RequestBody RequestDTO dto ) {
        return service.createExpense(dto);
    }

    @GetMapping
    public List<ResponseDTO> getAllExpenses() {
        return service.getAllExpenses();
    }

    @GetMapping({"/id"})
    public ResponseDTO getExpenseById(@PathVariable Long id) {
        return service.getExpenseById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteExpense(@PathVariable Long id) {
        service.deleteExpense(id);
        return "Expense deleted successfully";
    }
}
