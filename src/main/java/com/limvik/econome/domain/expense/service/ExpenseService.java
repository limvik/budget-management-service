package com.limvik.econome.domain.expense.service;

import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.infrastructure.expense.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public Expense createExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(Expense expense) {
        if (expenseRepository.existsByUserAndId(expense.getUser(), expense.getId()))
            return expenseRepository.save(expense);
        else
            throw new ErrorException(ErrorCode.NOT_EXIST_EXPENSE);
    }

    @Transactional(readOnly = true)
    public Expense getExpense(long userId, long expenseId) {
        var user = User.builder().id(userId).build();
        return expenseRepository.findByUserAndId(user, expenseId).orElseThrow(
                () -> new ErrorException(ErrorCode.NOT_EXIST_EXPENSE));
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpenses(long userId, LocalDate startDate, LocalDate endDate, Long minAmount, Long maxAmount) {
        if (maxAmount <= 0) maxAmount = Long.MAX_VALUE;
        String startInstant = startDate.toString() + "T00:00:00Z";
        String postfixForEndInstant = endDate.toString() + "T23:59:59Z";
        return expenseRepository.findAllExpenseList(userId,
                Instant.parse(startInstant), Instant.parse(postfixForEndInstant), minAmount, maxAmount);
    }

    @Transactional
    public void deleteExpense(long userId, long expenseId) {
        User user = User.builder().id(userId).build();
        Expense expense = expenseRepository.findByUserAndId(user, expenseId).orElseThrow(
                () -> new ErrorException(ErrorCode.NOT_EXIST_EXPENSE));
        expenseRepository.delete(expense);
    }
}
