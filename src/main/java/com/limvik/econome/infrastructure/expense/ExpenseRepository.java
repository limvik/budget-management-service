package com.limvik.econome.infrastructure.expense;

import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.expense.entity.ExpenseProjection;
import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByUserAndId(User user, long id);

    @Query("SELECT e FROM Expense e WHERE e.user.id = ?1 AND e.datetime BETWEEN ?2 AND ?3 AND e.amount BETWEEN ?4 AND ?5")
    List<Expense> findAllExpenseList(long userId, LocalDateTime startDate, LocalDateTime endDate, long minAmount, long maxAmount);

    @Query("SELECT e.category.id as categoryId, sum(e.amount) as amount " +
            "FROM Expense e " +
            "WHERE e.user.id = ?1 AND " +
            "FUNCTION('YEAR', e.datetime) = FUNCTION('YEAR', ?2) AND " +
            "FUNCTION('MONTH', e.datetime) = FUNCTION('MONTH', ?2) AND " +
            "FUNCTION('DAY', e.datetime) < FUNCTION('DAY', ?2) " +
            "GROUP BY e.category.id " +
            "ORDER BY categoryId DESC")
    List<Map<String, Long>> findThisMonthExpensesPerCategory(long userId, LocalDate date);

    @Query("SELECT e.category.id as categoryId, sum(e.amount) as amount " +
            "FROM Expense e " +
            "WHERE e.user.id = ?1 AND " +
            "FUNCTION('YEAR', e.datetime) = FUNCTION('YEAR', CURRENT_DATE) AND " +
            "FUNCTION('MONTH', e.datetime) = FUNCTION('MONTH', CURRENT_DATE) " +
            "AND FUNCTION('DAY', e.datetime) = FUNCTION('DAY', CURRENT_DATE) " +
            "GROUP BY e.category.id " +
            "ORDER BY categoryId DESC")
    List<Map<String, Long>> findTodayExpensesPerCategory(long userId);

    @Query("SELECT e.category.id as categoryId, sum(e.amount) as amount " +
            "FROM Expense e " +
            "WHERE e.user.id = ?1 AND " +
            "FUNCTION('YEAR', e.datetime) = FUNCTION('YEAR', ?2) AND " +
            "FUNCTION('MONTH', e.datetime) = FUNCTION('MONTH', ?2) " +
            "AND FUNCTION('DAY', e.datetime) <= FUNCTION('DAY', ?2) " +
            "GROUP BY e.category.id " +
            "ORDER BY categoryId ASC")
    List<ExpenseProjection.SumCategory> findMonthlyExpensesPerCategoryByYearAndMonthAndDayBefore(long userId,
                                                                                                 LocalDate yearMonthDay);

    @Query("SELECT e.category.id as categoryId, sum(e.amount) as amount " +
            "FROM Expense e " +
            "WHERE e.user.id = ?1 AND " +
            "FUNCTION('YEAR', e.datetime) = FUNCTION('YEAR', ?2) AND " +
            "FUNCTION('MONTH', e.datetime) = FUNCTION('MONTH', ?2) AND " +
            "FUNCTION('DAY', e.datetime) = FUNCTION('DAY', ?2) " +
            "GROUP BY e.category.id " +
            "ORDER BY categoryId ASC")
    List<ExpenseProjection.SumCategory> findWeeklyExpensesPerCategoryByYearAndMonthAndDay(long userId,
                                                                                          LocalDate yearMonthDay);

    @Query("SELECT sum(userExpense.amount) / sum(otherUserExpense.amount) * 100 as rate " +
            "FROM Expense userExpense, Expense otherUserExpense " +
            "WHERE userExpense.user.id = ?1 AND otherUserExpense.user.id != ?1 AND " +
            "FUNCTION('YEAR', userExpense.datetime) = FUNCTION('YEAR', CURRENT_DATE) AND " +
            "FUNCTION('MONTH', userExpense.datetime) = FUNCTION('MONTH', CURRENT_DATE) AND " +
            "FUNCTION('DAY', userExpense.datetime) <= FUNCTION('DAY', CURRENT_DATE)")
    Double findExpenseRateCompareOtherUser(long userId);

}
