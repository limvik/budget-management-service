package com.limvik.econome.web.budgetplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.io.Serializable;

public record BudgetPlanRequest(
        @Min(1)
        @Max(12)
        long categoryId,
        long amount
) implements Serializable { }
