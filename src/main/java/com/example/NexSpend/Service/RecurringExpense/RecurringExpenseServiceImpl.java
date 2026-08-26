package com.example.NexSpend.Service.RecurringExpense;

import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseRequestDTO;
import com.example.NexSpend.DTO.RecurringExpenseDTO.RecurringExpenseResponseDTO;
import com.example.NexSpend.Entity.*;
import com.example.NexSpend.Exception.UserNotFoundException;
import com.example.NexSpend.Mapper.RecurringExpenseMapper;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.RecurringExpenseRepository;
import com.example.NexSpend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurringExpenseServiceImpl implements RecurringExpenseService{
    private final RecurringExpenseRepository recurringRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseMapper recurringExpenseMapper;

    @Override
    public RecurringExpenseResponseDTO createRecurring(
            RecurringExpenseRequestDTO dto,
            Authentication auth) {

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecurringExpense recurring = RecurringExpense.builder()
                .user(user)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .category(Category.valueOf(dto.getCategory().toUpperCase()))
                .type(ExpenseType.valueOf(dto.getType().toUpperCase()))
                .frequency(Frequency.valueOf(dto.getFrequency().toUpperCase()))
                .nextExecutionDate(dto.getStartDate())  // Convert to LocalDateTime
                .active(true)
                .build();

        RecurringExpense saved = recurringRepository.save(recurring);
        return recurringExpenseMapper.mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponseDTO> getUserRecurring(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return recurringRepository.findByUserAndActiveTrue(user)
                .stream()
                .map(recurringExpenseMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processDueRecurringExpenses() {
        LocalDateTime today = LocalDateTime.now();
        List<RecurringExpense> dueList = recurringRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(today);

        for (RecurringExpense recurring : dueList) {
            // Create actual expense
            Expense expense = Expense.builder()
                    .description(recurring.getDescription() + " (Recurring)")
                    .amount(recurring.getAmount())
                    .category(recurring.getCategory())
                    .type(recurring.getType())
                    .date(today)
                    .user(recurring.getUser())
                    .build();
            expenseRepository.save(expense);

            LocalDateTime nextDate = switch (recurring.getFrequency()) {
                case MONTHLY -> recurring.getNextExecutionDate().plusMonths(1);
                case WEEKLY  -> recurring.getNextExecutionDate().plusWeeks(1);
                case YEARLY  -> recurring.getNextExecutionDate().plusYears(1);
            };

            recurring.setNextExecutionDate(nextDate);
            recurringRepository.save(recurring);
        }
    }
}
