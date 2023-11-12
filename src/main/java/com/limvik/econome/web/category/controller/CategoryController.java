package com.limvik.econome.web.category.controller;

import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.web.category.dto.CategoryListResponse;
import com.limvik.econome.web.category.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @GetMapping
    public ResponseEntity<CategoryListResponse> getCategories() {
        return ResponseEntity.ok(new CategoryListResponse(getCategoryList()));
    }

    private List<CategoryResponse> getCategoryList() {
        List<CategoryResponse> categoryResponses = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        Arrays.asList(BudgetCategory.values()).forEach(category ->
            categoryResponses.add(new CategoryResponse(counter.incrementAndGet(), category.getCategory())));
        return categoryResponses;
    }

}
