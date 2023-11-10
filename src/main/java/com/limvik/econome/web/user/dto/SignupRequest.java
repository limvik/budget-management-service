package com.limvik.econome.web.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Length;

public record SignupRequest(
        @Length(max = 20)
        String username,

        @Email
        String email,

        @Length(min = 8, max = 64)
        String password,

        @Min(1000L)
        long minimumDailyExpense,

        boolean agreeAlarm
) { }
