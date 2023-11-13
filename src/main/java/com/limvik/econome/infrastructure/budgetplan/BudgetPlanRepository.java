package com.limvik.econome.infrastructure.budgetplan;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    boolean existsByUserAndDateAndCategory(User user, LocalDate date, Category category);

    List<BudgetPlan> findAllByUserAndDate(User user, LocalDate date);

}
