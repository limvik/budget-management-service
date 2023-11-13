package com.limvik.econome.domain.budgetplan.service;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.infrastructure.budgetplan.BudgetPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BudgetPlanService {

    private final BudgetPlanRepository budgetPlanRepository;

    @Transactional
    public List<BudgetPlan> createBudgetPlans(List<BudgetPlan> budgetPlans) {
        if (isNotExistPlan(budgetPlans.get(0))) {
            return budgetPlanRepository.saveAll(budgetPlans);
        } else {
            throw new ErrorException(ErrorCode.DUPLICATED_BUDGET_PLAN);
        }
    }

    @Transactional
    public void updateBudgetPlans(List<BudgetPlan> budgetPlans) {
        budgetPlans.forEach(budgetPlan ->{
            if (isNotExistPlan(budgetPlan))  {
                throw new ErrorException(ErrorCode.NOT_EXIST_BUDGET_PLAN);
            } else {
                budgetPlanRepository.updateAmountByUserAndDateAndCategory(
                        budgetPlan.getUser().getId(),
                        budgetPlan.getCategory().getId(),
                        budgetPlan.getDate(),
                        budgetPlan.getAmount());
            }
        });
    }

    private boolean isNotExistPlan(BudgetPlan budgetPlan) {
        return !budgetPlanRepository.existsByUserAndDateAndCategory(
                budgetPlan.getUser(), budgetPlan.getDate(), budgetPlan.getCategory());
    }

    @Transactional
    public List<BudgetPlan> getBudgetPlans(long userId, LocalDate date) {
        var user = User.builder().id(userId).build();
        return budgetPlanRepository.findAllByUserAndDate(user, date);
    }
}
