package com.limvik.econome.web.budgetplan.controller;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.budgetplan.service.BudgetPlanService;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListRequest;
import com.limvik.econome.web.util.UserUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/budget-plans")
public class BudgetPlanController {

    private final BudgetPlanService budgetPlanService;

    @PostMapping
    public ResponseEntity<String> createBudgetPlan(@Valid @RequestParam @Min(1) @Max(9999) int year,
                                                 @Valid @RequestParam @Min(1) @Max(12) int month,
                                                 @Valid @RequestBody BudgetPlanListRequest budgetPlans,
                                                 Authentication authentication) {
        var token = (JwtAuthenticationToken) authentication;
        var date = LocalDate.of(year, month, 1);
        long userId = UserUtil.getUserIdFromJwt(token);
        List<BudgetPlan> budgetPlanList = mapRequestToBudgetPlanList(budgetPlans, userId, date);

        budgetPlanList = budgetPlanService.createBudgetPlans(budgetPlanList);

        year = budgetPlanList.get(0).getDate().getYear();
        month = budgetPlanList.get(0).getDate().getMonthValue();
        String location = "/api/v1/budget-plans?year=%d&month=%d".formatted(year, month);
        return ResponseEntity.created(URI.create(location)).build();
    }

    private List<BudgetPlan> mapRequestToBudgetPlanList(BudgetPlanListRequest budgetPlans, long userId, LocalDate date) {
        return budgetPlans.budgetPlans().stream()
                .map(budgetPlanRequest -> BudgetPlan.builder()
                        .date(date)
                        .user(User.builder().id(userId).build())
                        .category(Category.builder().id(budgetPlanRequest.categoryId()).build())
                        .amount(budgetPlanRequest.amount())
                        .build())
                .toList();
    }
}
