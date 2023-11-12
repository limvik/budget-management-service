package com.limvik.econome.web.budgetplan.dto;

import java.io.Serializable;
import java.util.List;

public record BudgetPlanListRequest(
        List<BudgetPlanRequest> budgetPlans
) implements Serializable { }
