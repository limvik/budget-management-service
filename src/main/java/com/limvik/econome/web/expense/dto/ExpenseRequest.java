package com.limvik.econome.web.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ExpenseRequest(
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        LocalDateTime datetime,
        @NotNull
        Long categoryId,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Long amount,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Length(max = 60)
        String memo,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Boolean excluded
) implements Serializable { }
