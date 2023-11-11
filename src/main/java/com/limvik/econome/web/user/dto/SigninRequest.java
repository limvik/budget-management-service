package com.limvik.econome.web.user.dto;

import org.hibernate.validator.constraints.Length;

public record SigninRequest(
        @Length(max = 20)
        String username,

        @Length(min = 8, max = 64)
        String password
) { }
