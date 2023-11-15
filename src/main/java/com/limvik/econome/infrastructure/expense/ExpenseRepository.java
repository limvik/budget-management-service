package com.limvik.econome.infrastructure.expense;

import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByUserAndId(User user, long id);

    boolean existsByUserAndId(User user, long id);

    @Query("SELECT e FROM Expense e WHERE e.user.id = ?1 AND e.datetime BETWEEN ?2 AND ?3 AND e.amount BETWEEN ?4 AND ?5")
    List<Expense> findAllExpenseList(long userId, Instant startDate, Instant endDate, long minAmount, long maxAmount);

    @Query("SELECT e.category.id as categoryId, sum(e.amount) as amount " +
            "FROM Expense e " +
            "WHERE e.user.id = ?1 AND " +
            "FUNCTION('YEAR', e.datetime) = FUNCTION('YEAR', CURRENT_DATE) AND " +
            "FUNCTION('MONTH', e.datetime) = FUNCTION('MONTH', CURRENT_DATE) AND " +
            "FUNCTION('DAY', e.datetime) < FUNCTION('DAY', CURRENT_DATE) " +
            "GROUP BY e.category.id " +
            "ORDER BY categoryId DESC")
    List<Map<String, Long>> findThisMonthExpensesPerCategory(long userId);

    @Query("SELECT e.category.id as categoryId, sum(e.amount) as amount " +
            "FROM Expense e " +
            "WHERE e.user.id = ?1 AND " +
            "FUNCTION('YEAR', e.datetime) = FUNCTION('YEAR', CURRENT_DATE) AND " +
            "FUNCTION('MONTH', e.datetime) = FUNCTION('MONTH', CURRENT_DATE) " +
            "AND FUNCTION('DAY', e.datetime) = FUNCTION('DAY', CURRENT_DATE) " +
            "GROUP BY e.category.id " +
            "ORDER BY categoryId DESC")
    List<Map<String, Long>> findTodayExpensesPerCategory(long userId);
}
