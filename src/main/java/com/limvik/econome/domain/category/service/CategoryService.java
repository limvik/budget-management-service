package com.limvik.econome.domain.category.service;

import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.infrastructure.category.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @PostConstruct
    private void saveCategory() {
        if (categoryRepository.count() == 0) {
            List<Category> categories = new ArrayList<>();
            Arrays.asList(BudgetCategory.values()).forEach(category ->
                    categories.add(Category.builder().name(category).build()));
            categoryRepository.saveAll(categories);
        }
    }
}
