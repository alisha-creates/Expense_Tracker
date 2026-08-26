package com.example.NexSpend.Repository;

import com.example.NexSpend.Entity.RecurringExpense;
import com.example.NexSpend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findByUserAndActiveTrue(User user);

    List<RecurringExpense> findByActiveTrueAndNextExecutionDateLessThanEqual(LocalDateTime date);

    List<RecurringExpense>
    findByUserAndActiveTrueAndNextExecutionDateBetween(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    @Modifying
    @Query("DELETE FROM RecurringExpense r WHERE r.user.id = :userId")
    void permanentlyDeleteByUserId(@Param("userId") Long userId);

    Optional<RecurringExpense> findByIdAndDeletedFalse(Long id);
}
