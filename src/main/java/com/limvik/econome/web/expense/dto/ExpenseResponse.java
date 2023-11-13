package com.limvik.econome.web.expense.dto;

import java.io.Serializable;
import java.time.Instant;

public record ExpenseResponse(
        long id,
        Instant datetime,
        long categoryId,
        long amount,
        String memo,
        boolean excluded
) implements Serializable { }
