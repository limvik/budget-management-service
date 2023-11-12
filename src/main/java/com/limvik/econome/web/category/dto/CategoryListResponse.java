package com.limvik.econome.web.category.dto;

import java.io.Serializable;
import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> categories
) implements Serializable { }
