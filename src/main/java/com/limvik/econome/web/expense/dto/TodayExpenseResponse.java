package com.limvik.econome.web.expense.dto;

import java.io.Serializable;

public record TodayExpenseResponse(
        Long categoryId,
        String categoryName,
        Long recommendedAmount,
        Long spentAmount,
        String risk
) implements Serializable { }
