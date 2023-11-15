package com.limvik.econome.web.expense.dto;

import java.io.Serializable;
import java.util.List;

public record TodayExpenseListResponse(
        Long spentTotalAmount,
        List<TodayExpenseResponse> details
) implements Serializable { }
