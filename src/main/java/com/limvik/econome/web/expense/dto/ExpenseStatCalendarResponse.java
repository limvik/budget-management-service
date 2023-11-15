package com.limvik.econome.web.expense.dto;

import java.io.Serializable;
import java.util.List;

public record ExpenseStatCalendarResponse(
        String totalExpenseRate,
        List<ExpenseStatCalendarCategoryResponse> details
) implements Serializable { }
