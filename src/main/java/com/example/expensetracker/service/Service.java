package com.example.expensetracker.service;

import com.example.expensetracker.dto.*;

import java.util.List;

public interface Service {
     ResponseDTO createExpense(RequestDTO dto);

    List<ResponseDTO> getAllExpenses();

    ResponseDTO getExpenseById(Long id);

    void deleteExpense(Long id);
}
