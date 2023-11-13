package com.limvik.econome.infrastructure.budgetplan;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    boolean existsByUserAndDateAndCategory(User user, LocalDate date, Category category);

    List<BudgetPlan> findAllByUserAndDate(User user, LocalDate date);

    @Modifying
    @Query("UPDATE BudgetPlan bp SET bp.amount = ?4 " +
            "WHERE bp.user.id = ?1 " +
            "AND bp.category.id = ?2 " +
            "AND bp.date = ?3")
    void updateAmountByUserAndDateAndCategory(long userId, long categoryId, LocalDate date, long amount);

}
