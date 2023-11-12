package com.limvik.econome.infrastructure.budgetplan;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    boolean existsByDateAndCategory(LocalDate date, Category category);

}
