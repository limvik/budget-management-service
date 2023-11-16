package com.limvik.econome.web.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        LocalDateTime datetime,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Long categoryId,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        String categoryName,
        Long amount,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        String memo,
        Boolean excluded
) implements Serializable { }
