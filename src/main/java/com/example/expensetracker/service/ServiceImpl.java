package com.example.expensetracker.service;

import com.example.expensetracker.dto.*;
import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.mapper.Mapper;
import com.example.expensetracker.repository.Repository;
import com.example.expensetracker.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ServiceImpl implements Service{
    @Autowired
    Repository repository;

    public ResponseDTO createExpense(RequestDTO dto) {
        Expense expense = Mapper.toEntity(dto);
        Expense saved = repository.save(expense);
        return Mapper.toDTO(saved);
    }

    public List<ResponseDTO> getAllExpenses() {
        return repository.findAll()
                .stream()
                .map(Mapper::toDTO)
                .collect(Collectors.toList());
    }

    public ResponseDTO getExpenseById(Long id) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return Mapper.toDTO(expense);
    }

    public void deleteExpense(Long id) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        repository.delete(expense);
    }
}
