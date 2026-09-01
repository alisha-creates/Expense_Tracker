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


    @Query("""
       SELECT b
       FROM Budget b
       WHERE b.user = :user
       AND b.year = :year
       AND b.deleted = false
       """)
    List<Budget> findByUserAndYear(
            @Param("user") User user,
            @Param("year") Integer year
    );

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.year = :year AND b.month = :month")
    List<Budget> findByUserIdAndMonthAndYear(@Param("userId") Long userId,
                                             @Param("month") Integer month,
                                             @Param("year") Integer year);

    Optional<Budget> findByIdAndDeletedFalse(Long id);

    @Modifying
    @Query(value = "DELETE FROM budgets WHERE user_id = :userId", nativeQuery = true)
    void permanentlyDeleteByUserId(@Param("userId") Long userId);
}
