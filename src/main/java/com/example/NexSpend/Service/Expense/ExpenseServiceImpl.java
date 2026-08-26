package com.example.NexSpend.Service.Expense;

import com.example.NexSpend.DTO.ExpenseDTO.ExpenseRequestDTO;
import com.example.NexSpend.DTO.ExpenseDTO.ExpenseResponseDTO;
import com.example.NexSpend.Entity.Expense;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.ExpenseType;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Exception.ExpenseNotFoundException;
import com.example.NexSpend.Exception.UnauthorizedActionException;
import com.example.NexSpend.Exception.UserNotFoundException;
import com.example.NexSpend.Mapper.ExpenseMapper;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.UserRepository;
import com.example.NexSpend.Util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public ExpenseResponseDTO createExpense(ExpenseRequestDTO requestDTO) {

        String email = UserUtil.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Expense expense = Expense.builder()
                .description(requestDTO.getDescription())
                .amount(requestDTO.getAmount())
                .category(Category.valueOf(requestDTO.getCategory()))
                .type(ExpenseType.valueOf(requestDTO.getType()))
                .date(requestDTO.getDate())
                .user(user)
                .build();

        Expense saved = expenseRepository.save(expense);

        return expenseMapper.mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponseDTO getExpenseById(Long id) {

        String email = UserUtil.getCurrentUserEmail();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email)) {
            throw new UnauthorizedActionException("Unauthorized access");
        }

        return expenseMapper.mapToDto(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponseDTO> getAllExpenses(Pageable pageable) {

        String email = UserUtil.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        return expenseRepository.findByUserId(user.getId(), pageable)
                .map(expenseMapper::mapToDto);
    }

    @Override
    @Transactional
    public ExpenseResponseDTO updateExpense(Long id,
                                            ExpenseRequestDTO requestDTO) {

        String email = UserUtil.getCurrentUserEmail();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email)) {
            throw new UnauthorizedActionException("Unauthorized access");
        }

        expense.setDescription(requestDTO.getDescription());
        expense.setAmount(requestDTO.getAmount());
        expense.setCategory(Category.valueOf(requestDTO.getCategory()));
        expense.setType(ExpenseType.valueOf(requestDTO.getType()));
        expense.setDate(requestDTO.getDate());

        Expense updated = expenseRepository.save(expense);

        return expenseMapper.mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {

        String email = UserUtil.getCurrentUserEmail();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email)) {
            throw new UnauthorizedActionException("Unauthorized access");
        }

        expense.setDeleted(true);
        expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponseDTO> filterExpenses(Category category,
                                                   ExpenseType type,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   Pageable pageable) {

        String email = UserUtil.getCurrentUserEmail();

        Specification<Expense> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("email"), email));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (startDate != null && endDate != null) {
                predicates.add(cb.between(root.get("date"), startDate, endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return expenseRepository.findAll(spec, pageable)
                .map(expenseMapper::mapToDto);
    }
}
