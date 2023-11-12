package com.limvik.econome.web.category.dto;

import java.io.Serializable;

public record CategoryResponse(
        long id,
        String name
) implements Serializable { }
