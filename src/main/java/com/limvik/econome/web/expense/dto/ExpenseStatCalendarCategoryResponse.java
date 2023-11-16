package com.limvik.econome.web.expense.dto;

import java.io.Serializable;

public record ExpenseStatCalendarCategoryResponse(
        Long categoryId,
        String categoryName,
        String expenseRate
) implements Serializable { }
