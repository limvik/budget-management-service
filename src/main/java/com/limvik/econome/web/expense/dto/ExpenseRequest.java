package com.limvik.econome.web.expense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.time.Instant;

public record ExpenseRequest(

        @NotNull
        Instant datetime,
        @NotNull
        Long categoryId,
        @NotNull
        Long amount,
        @Length(max = 60)
        String memo,
        @JsonProperty(defaultValue = "false")
        boolean excluded
) implements Serializable { }
