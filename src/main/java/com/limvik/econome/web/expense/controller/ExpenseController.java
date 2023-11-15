package com.limvik.econome.web.expense.controller;

import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.expense.service.ExpenseService;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.web.expense.dto.*;
import com.limvik.econome.web.util.UserUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<String> createExpense(@Valid @RequestBody ExpenseRequest expenseRequest,
                                                Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        Expense createdExpense = expenseService.createExpense(mapRequestToEntity(expenseRequest, userId, null));
        return ResponseEntity.created(URI.create("/api/v1/expenses/" + createdExpense.getId())).build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@Valid @PathVariable(name = "id") @Min(1) Long expenseId,
                                                @Valid @RequestBody ExpenseRequest expenseRequest,
                                                Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        expenseService.updateExpense(mapRequestToEntity(expenseRequest, userId, expenseId));
        return ResponseEntity.ok().build();
    }

    private Expense mapRequestToEntity(ExpenseRequest expenseRequest, Long userId, Long expenseId) {
        return Expense.builder()
                .id(expenseId)
                .user(User.builder().id(userId).build())
                .category(Category.builder().id(expenseRequest.categoryId()).build())
                .amount(expenseRequest.amount())
                .memo(expenseRequest.memo())
                .datetime(expenseRequest.datetime())
                .excluded(expenseRequest.excluded())
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@Valid @PathVariable(name = "id") @Min(1) long expenseId,
                                                      Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        Expense expense = expenseService.getExpense(userId, expenseId);
        ExpenseResponse expenseResponse = mapEntityToResponse(expense);
        return ResponseEntity.ok(expenseResponse);
    }

    /**
     * 지정된 기간, 카테고리, 지출금액 범위에 속한 사용자의 모든 지출 기록 리스트를 반환합니다.
     * 반환되는 항목은 지정한 카테고리만 반환합니다.
     * 총액 합계에는 지정된 기간의 모든 지출 기록(카테고리 무관)의 지출 합계입니다.
     * 카테고리별 합계는 총액 합계에서 사용자가 지정한 카테고리의 지출 합계입니다.
     * @param startDate 시작 일자
     * @param endDate 종료 일자
     * @param categoryId 카테고리 식별자
     * @param minAmount 최소 금액
     * @param maxAmount 최대 금액
     * @param authentication 인증된 사용자 정보
     * @return 지정된 기간, 카테고리, 지출금액 범위에 속한 사용자의 지출 기록 반환
     */
    @GetMapping
    public ResponseEntity<ExpenseListResponse> getExpenseList(@Valid @RequestParam LocalDate startDate,
                                                              @Valid @RequestParam LocalDate endDate,
                                                              @Valid @RequestParam Long categoryId,
                                                              @Valid @Min(0) @RequestParam(required = false, defaultValue = "0") Long minAmount,
                                                              @Valid @Min(0) @RequestParam(required = false, defaultValue = "0") Long maxAmount,
                                                              Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        List<Expense> expenses = expenseService.getExpenses(userId, startDate, endDate, minAmount, maxAmount);

        ExpenseListResponse expenseListResponse = new ExpenseListResponse(
                mapEntityListToResponseList(expenses, categoryId),
                getTotalAmount(expenses),
                getTotalAmountForCategory(expenses, categoryId));
        return ResponseEntity.ok(expenseListResponse);
    }

    /**
     * 사용자가 지정한 기간, 금액의 범위에 포함된 지출 기록과 카테고리 식별자를 받아 지출 기록 반환용 DTO 리스트로 변환하여 반환합니다.
     * @param expenses 지출 기록 도메인 객체 리스트
     * @param categoryId 카테고리 식별자
     * @return 지출 기록 반환용 DTO 리스트 반환
     */
    private List<ExpenseResponse> mapEntityListToResponseList(List<Expense> expenses, Long categoryId) {
        return expenses.stream()
                .filter(expense -> Objects.equals(expense.getCategory().getId(), categoryId))
                .map(this::mapEntityToResponse)
                .toList();
    }

    /**
     * 사용자가 지정한 기간, 금액의 범위에 포함된 지출 기록을 받아 합계 제외 항목을 제외한 총액 합계를 반환합니다.
     * @param expenses 지출 기록 도메인 객체 리스트
     * @return 합계 제외 항목을 제외한 총액 합계 반환
     */
    private long getTotalAmount(List<Expense> expenses) {
        return expenses.stream()
                .filter(expense -> !expense.isExcluded())
                .mapToLong(Expense::getAmount)
                .sum();
    }

    /**
     * 사용자가 지정한 기간, 금액의 범위에 포함된 지출 기록과 카테고리 식별자를 받아
     * 합계 제외 항목을 제외한 카테고리별 총액 합계를 반환합니다.
     * @param expenses 지출 기록 도메인 객체 리스트
     * @param categoryId 카테고리 식별자
     * @return 합계 제외항목을 제외한 카테고리별 총액 합계 반환
     */
    private long getTotalAmountForCategory(List<Expense> expenses, Long categoryId) {
        return expenses.stream()
                .filter(expense -> expense.getCategory().getId().equals(categoryId) && !expense.isExcluded())
                .mapToLong(Expense::getAmount)
                .sum();
    }

    private ExpenseResponse mapEntityToResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDatetime(),
                expense.getCategory().getId(),
                expense.getCategory().getName().getCategory(),
                expense.getAmount(),
                expense.getMemo(),
                expense.isExcluded()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@Valid @PathVariable(name = "id") @Min(1) long expenseId,
                                                Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationExpenseListResponse> getTodayRecommendationExpenses(Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        Map<Long, Long> recommendedTodayExpenseAmountPerCategory = expenseService.getTodayRecommendationExpenses(userId);
        return ResponseEntity.ok(mapEntityListToRecommendationResponseList(recommendedTodayExpenseAmountPerCategory));
    }

    private RecommendationExpenseListResponse mapEntityListToRecommendationResponseList(
            Map<Long, Long> recommendedTodayExpenseAmountPerCategory) {
        long recommendedTodayTotalAmount = recommendedTodayExpenseAmountPerCategory.values()
                .stream().reduce(0L, Long::sum);
        String message = getRecommendExpenseMessage();
        List<RecommendationExpenseResponse> recommendationExpenseResponse = new ArrayList<>();
        recommendedTodayExpenseAmountPerCategory.forEach((categoryId, amount) -> recommendationExpenseResponse.add(
                new RecommendationExpenseResponse(categoryId,
                        BudgetCategory.values()[categoryId.intValue() - 1].getCategory(),
                        amount)));

        return new RecommendationExpenseListResponse(
                recommendedTodayTotalAmount,
                message,
                recommendationExpenseResponse
        );
    }

    private String getRecommendExpenseMessage() {
        return LocalDate.now().getDayOfMonth() == 1 ?
                "1일 이네요! 이번달도 새로운 마음으로 체계적인 지출 도전!"
                : "오늘도 합리적인 소비 생활 화이팅!";
    }

    @GetMapping("/today")
    public ResponseEntity<TodayExpenseListResponse> getTodayExpenses(Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        Map<Long, Long> todayExpenseAmountPerCategory = expenseService.getTodayExpenses(userId);
        Map<Long, Long> recommendedTodayExpenseAmountPerCategory = expenseService.getTodayRecommendationExpenses(userId);
        return ResponseEntity.ok(mapEntityToTodayExpenseResponseList(
                todayExpenseAmountPerCategory,
                recommendedTodayExpenseAmountPerCategory));
    }

    private TodayExpenseListResponse mapEntityToTodayExpenseResponseList(
            Map<Long, Long> todayExpenseAmountPerCategory,
            Map<Long, Long> recommendedTodayExpenseAmountPerCategory){

        List<TodayExpenseResponse> details = new ArrayList<>();
        todayExpenseAmountPerCategory.forEach((categoryId, spentAmount) -> {
            long recommendedAmount = recommendedTodayExpenseAmountPerCategory.containsKey(categoryId) ?
                    recommendedTodayExpenseAmountPerCategory.get(categoryId) : 0;
            String risk = getRisk(recommendedAmount, spentAmount);
            details.add(new TodayExpenseResponse(
                        categoryId,
                        BudgetCategory.values()[categoryId.intValue() - 1].getCategory(),
                        recommendedAmount,
                        spentAmount,
                        risk));
        });
        return new TodayExpenseListResponse(
                todayExpenseAmountPerCategory.values().stream().reduce(0L, Long::sum),
                details);
    }

    private String getRisk(long recommendedAmount, long spentAmount) {
        if (recommendedAmount == 0) return "0%";
        return (long)((double)spentAmount / recommendedAmount * 100) + "%";
    }

}
