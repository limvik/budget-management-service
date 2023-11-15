package com.limvik.econome.web.expense.dto;

import java.io.Serializable;

public record RecommendationExpenseResponse(
        Long categoryId,
        String categoryName,
        Long amount
) implements Serializable { }
