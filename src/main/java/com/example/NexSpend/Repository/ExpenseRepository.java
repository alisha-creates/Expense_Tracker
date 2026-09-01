package com.example.NexSpend.Repository;

import com.example.NexSpend.Entity.Expense;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Entity.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    Page<Expense> findByUserId(Long userId, Pageable pageable);


    List<Expense> findTop10ByUserIdOrderByDateDesc(Long userId);

    List<Expense> findByUserAndDateBetween(
            User user,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    @Modifying
    @Query(value = "DELETE FROM expenses WHERE user_id = :userId", nativeQuery = true)
    void permanentlyDeleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.type = :type")
    BigDecimal sumByUserIdAndType(@Param("userId") Long userId, @Param("type") ExpenseType type);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND e.date BETWEEN :startDate AND :endDate ")
    BigDecimal sumTodayExpense(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal sumMonthlyExpense(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(e.date), SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND e.date >= :startDate GROUP BY DATE(e.date) ORDER BY DATE(e.date)")
    List<Object[]> expenseTrend(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    // Top category
    @Query("SELECT e.category, SUM(e.amount) as total FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' GROUP BY e.category ORDER BY total DESC")
    List<Object[]> topCategories(@Param("userId") Long userId);

    @Query("""
    SELECT COALESCE(SUM(e.amount), 0)
    FROM Expense e
    WHERE e.user.id = :userId
    AND e.type = 'EXPENSE'
    AND e.category = :category
    AND MONTH(e.date) = :month
    AND YEAR(e.date) = :year
""")
    BigDecimal sumByUserIdAndTypeAndCategoryAndDateBetween(
            @Param("userId") Long userId,
            @Param("category") Category category,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e " +
            "WHERE e.user.id = :userId AND e.type = 'EXPENSE' " +
            "GROUP BY e.category")
    List<Object[]> sumExpenseByCategory(@Param("userId") Long userId);

    @Query("SELECT DATE(e.date), SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND e.date BETWEEN :startDate AND :endDate GROUP BY DATE(e.date) ORDER BY DATE(e.date)")
    List<Object[]> expenseTrendBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
