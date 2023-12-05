package com.limvik.econome.infrastructure.budgetplan;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.budgetplan.entity.BudgetPlanProjection;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.util.Streamable;

import java.time.LocalDate;
import java.util.List;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    @Query("SELECT COUNT(*) " +
            "FROM BudgetPlan bp " +
            "WHERE bp.user = ?1 " +
            "AND bp.date = ?2 " +
            "AND bp.category IN (?3)")
    long countByUserAndDateAndCategories(User user, LocalDate date, List<Category> categories);

    List<BudgetPlan> findAllByUserAndDate(User user, LocalDate date);

    @Modifying
    @Query("UPDATE BudgetPlan bp SET bp.amount = ?4 " +
            "WHERE bp.user.id = ?1 " +
            "AND bp.category.id = ?2 " +
            "AND bp.date = ?3")
    void updateAmountByUserAndDateAndCategory(long userId, long categoryId, LocalDate date, long amount);

    /**
     * 전체 서비스 이용자의 카테고리별 평균 비율에 기반한 카테고리별 추천 금액 목록을 반환합니다.
     * @param amount 사용자의 총 예산
     * @return 카테고리별 추천 금액 목록
     */
    @Query("SELECT new com.limvik.econome.domain.budgetplan.entity.BudgetPlan(bp.category.id, bp.category.name, " +
            "(avg(bp.amount) / (SELECT sum(bp2.amount) FROM BudgetPlan bp2) * :amount) as amount) " +
            "FROM BudgetPlan bp " +
            "GROUP BY bp.category.id, bp.category.name")
    List<BudgetPlan> findRecommendedBudgetPlans(long amount);

    @Query("SELECT bp.category.id as categoryId, sum(bp.amount) as amount " +
            "FROM BudgetPlan bp " +
            "WHERE bp.user.id = ?1 AND " +
            "FUNCTION('YEAR', bp.date) = FUNCTION('YEAR', ?2) AND " +
            "FUNCTION('MONTH', bp.date) = FUNCTION('MONTH', ?2) " +
            "GROUP BY bp.category.id")
    Streamable<BudgetPlanProjection.SumCategory> findThisMonthBudgetPerCategory(long userId, LocalDate date);

}
