package com.limvik.econome.web.budgetplan.controller;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.budgetplan.service.BudgetPlanService;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListRequest;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListResponse;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanResponse;
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
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/budget-plans")
public class BudgetPlanController {

    private final BudgetPlanService budgetPlanService;

    @PostMapping
    public ResponseEntity<String> createBudgetPlans(@Valid @RequestParam @Min(1) @Max(9999) int year,
                                                 @Valid @RequestParam @Min(1) @Max(12) int month,
                                                 @Valid @RequestBody BudgetPlanListRequest budgetPlans,
                                                 Authentication authentication) {
        long userId = getUserIdFromAuthentication(authentication);
        var date = LocalDate.of(year, month, 1);
        List<BudgetPlan> budgetPlanList = mapRequestToBudgetPlanList(budgetPlans, userId, date);

        budgetPlanList = budgetPlanService.createBudgetPlans(budgetPlanList);

        year = budgetPlanList.get(0).getDate().getYear();
        month = budgetPlanList.get(0).getDate().getMonthValue();
        String location = "/api/v1/budget-plans?year=%d&month=%d".formatted(year, month);
        return ResponseEntity.created(URI.create(location)).build();
    }

    @GetMapping
    public ResponseEntity<BudgetPlanListResponse> getBudgetPlans(@Valid @RequestParam @Min(1) @Max(9999) int year,
                                                                 @Valid @RequestParam @Min(1) @Max(12) int month,
                                                                 Authentication authentication) {
        long userId = getUserIdFromAuthentication(authentication);
        var date = LocalDate.of(year, month, 1);
        List<BudgetPlan> budgetPlanList = budgetPlanService.getBudgetPlans(userId, date);
        return ResponseEntity.ok(mapBudgetPlanListToResponseList(budgetPlanList));
    }

    @PatchMapping
    public ResponseEntity<BudgetPlanListResponse> updateBudgetPlans(@Valid @RequestParam @Min(1) @Max(9999) int year,
                                                                    @Valid @RequestParam @Min(1) @Max(12) int month,
                                                                    @Valid @RequestBody BudgetPlanListRequest budgetPlans,
                                                                    Authentication authentication) {
        long userId = getUserIdFromAuthentication(authentication);
        var date = LocalDate.of(year, month, 1);
        budgetPlanService.updateBudgetPlans(mapRequestToBudgetPlanList(budgetPlans, userId, date));
        return ResponseEntity.ok().build();
    }

    private long getUserIdFromAuthentication(Authentication authentication) {
        var token = (JwtAuthenticationToken) authentication;
        return UserUtil.getUserIdFromJwt(token);
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

    @GetMapping("/recommendations")
    public ResponseEntity<BudgetPlanListResponse> getRecommendations(@Valid @RequestParam @Min(1) long amount) {
        List<BudgetPlan> budgetPlanList = budgetPlanService.getBudgetRecommendations(amount);
        return ResponseEntity.ok(mapBudgetPlanListToResponseList(budgetPlanList));
    }

    private BudgetPlanListResponse mapBudgetPlanListToResponseList(List<BudgetPlan> budgetPlanList) {
        List<BudgetPlanResponse> budgetPlanResponseList = new ArrayList<>();
        for (BudgetPlan budgetPlan : budgetPlanList) {
            BudgetPlanResponse budgetPlanResponse = new BudgetPlanResponse(
                    budgetPlan.getCategory().getId(),
                    budgetPlan.getCategory().getName().getCategory(),
                    budgetPlan.getAmount());
            budgetPlanResponseList.add(budgetPlanResponse);
        }
        return new BudgetPlanListResponse(budgetPlanResponseList);
    }

}
