package com.limvik.econome.infrastructure.expense;

import com.limvik.econome.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

}
