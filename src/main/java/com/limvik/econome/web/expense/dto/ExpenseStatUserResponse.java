package com.limvik.econome.web.expense.dto;

import java.io.Serializable;

public record ExpenseStatUserResponse(
        String relativeExpenseRate
) implements Serializable { }
