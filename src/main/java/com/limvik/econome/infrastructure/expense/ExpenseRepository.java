package com.limvik.econome.infrastructure.expense;

import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByUserAndId(User user, long id);

    boolean existsByUserAndId(User user, long id);

    @Query("SELECT e FROM Expense e WHERE e.user.id = ?1 AND e.datetime BETWEEN ?2 AND ?3 AND e.amount BETWEEN ?4 AND ?5")
    List<Expense> findAllExpenseList(long userId, Instant startDate, Instant endDate, long minAmount, long maxAmount);
}
