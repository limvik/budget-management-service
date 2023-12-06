package com.limvik.econome.domain.budgetplan.entity;

public class BudgetPlanProjection {

    public interface SumCategory {
        Long getCategoryId();
        Long getAmount();
    }

    public interface RecommendedBudget {
        Long getCategoryId();
        Long getAmount();
    }

}
