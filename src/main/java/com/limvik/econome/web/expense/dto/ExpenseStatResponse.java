package com.limvik.econome.web.expense.dto;

import java.io.Serializable;

public record ExpenseStatResponse(
        ExpenseStatCalendarResponse againstLastMonth,
        ExpenseStatCalendarResponse againstLastDayOfWeek,
        ExpenseStatUserResponse againstOtherUsers
) implements Serializable { }
