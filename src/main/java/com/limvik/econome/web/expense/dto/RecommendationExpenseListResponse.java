package com.limvik.econome.web.expense.dto;

import java.util.List;

public record RecommendationExpenseListResponse(
        Long recommendedTodayTotalAmount,
        String message,
        List<RecommendationExpenseResponse> recommendations
) {
}
