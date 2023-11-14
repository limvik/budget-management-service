package com.limvik.econome.web.expense.dto;

import java.io.Serializable;
import java.util.List;

public record ExpenseListResponse(
        List<ExpenseResponse> expenses,
        Long totalAmount,
        long totalAmountForCategory

) implements Serializable { }
