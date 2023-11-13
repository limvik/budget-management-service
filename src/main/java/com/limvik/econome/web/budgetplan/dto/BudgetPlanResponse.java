package com.limvik.econome.web.budgetplan.dto;

import java.io.Serializable;

public record BudgetPlanResponse(
        long categoryId,
        String categoryName,
        long amount
) implements Serializable { }
