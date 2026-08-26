package com.example.NexSpend.Repository;

import com.example.NexSpend.Entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.NexSpend.Entity.Category;
import com.example.NexSpend.Entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserAndCategoryAndMonthAndYear(User user, Category category, Integer month, Integer year);

    List<Budget> findByUserAndYear(User user, Integer year);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.year = :year AND b.month = :month")
    List<Budget> findByUserIdAndMonthAndYear(@Param("userId") Long userId,
                                             @Param("month") Integer month,
                                             @Param("year") Integer year);

    Optional<Budget> findByIdAndDeletedFalse(Long id);

    @Modifying
    @Query("DELETE FROM Budget b WHERE b.user.id = :userId")
    void permanentlyDeleteByUserId(@Param("userId") Long userId);
}
