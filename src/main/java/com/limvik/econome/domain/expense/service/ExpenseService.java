package com.limvik.econome.domain.expense.service;

import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.expense.entity.ExpenseProjection;
import com.limvik.econome.domain.expense.service.dto.CalendarStatDto;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.infrastructure.budgetplan.BudgetPlanRepository;
import com.limvik.econome.infrastructure.expense.ExpenseRepository;
import com.limvik.econome.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final UserRepository userRepository;

    @Transactional
    public Expense createExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(Expense expense) {
        if (expenseRepository.existsByUserAndId(expense.getUser(), expense.getId()))
            return expenseRepository.save(expense);
        else
            throw new ErrorException(ErrorCode.NOT_EXIST_EXPENSE);
    }

    @Transactional(readOnly = true)
    public Expense getExpense(long userId, long expenseId) {
        var user = User.builder().id(userId).build();
        return expenseRepository.findByUserAndId(user, expenseId).orElseThrow(
                () -> new ErrorException(ErrorCode.NOT_EXIST_EXPENSE));
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpenses(long userId, LocalDate startDate, LocalDate endDate, Long minAmount, Long maxAmount) {
        if (maxAmount <= 0) maxAmount = Long.MAX_VALUE;
        String startInstant = startDate.toString() + "T00:00:00Z";
        String postfixForEndInstant = endDate.toString() + "T23:59:59Z";
        return expenseRepository.findAllExpenseList(userId,
                Instant.parse(startInstant), Instant.parse(postfixForEndInstant), minAmount, maxAmount);
    }

    @Transactional
    public void deleteExpense(long userId, long expenseId) {
        User user = User.builder().id(userId).build();
        Expense expense = expenseRepository.findByUserAndId(user, expenseId).orElseThrow(
                () -> new ErrorException(ErrorCode.NOT_EXIST_EXPENSE));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getTodayRecommendationExpenses(long userId) {
        // 사용자 지정 월 예산 불러오기
        List<Map<String, Long>> monthlyBudget = budgetPlanRepository.findThisMonthBudgetPerCategory(userId);
        Map<Long, Long> monthlyBudgetMap = new HashMap<>();
        monthlyBudget.forEach(map -> monthlyBudgetMap.put(map.get("categoryId"), map.get("amount")));

        // 사용자 이번달 카테고리별 지출 합계 불러오기
        List<Map<String, Long>> monthlyExpenses = expenseRepository.findThisMonthExpensesPerCategory(userId);
        Map<Long, Long> monthlyExpensesMap = new HashMap<>();
        monthlyExpenses.forEach(map -> monthlyExpensesMap.put(map.get("categoryId"), map.get("amount")));

        // 카테고리별 추천 금액 계산하기
        int restDaysOfMonth = LocalDate.now().lengthOfMonth() - LocalDate.now().getDayOfMonth() + 1;
        long minimumDailyExpense = userRepository.findMinimumDailyExpenseById(userId);
        long expenseForNotCreatedBudgetPlan = getExpenseForNotCreatedBudgetPlan(monthlyBudgetMap, monthlyExpensesMap);
        long penaltyForUnexpectedExpensePerCategory = expenseForNotCreatedBudgetPlan / monthlyBudget.size();
        monthlyBudgetMap.forEach((categoryId, budget) -> {
            long monthlyExpensePerCategory = monthlyExpensesMap.getOrDefault(categoryId, 0L);
            long todayRecommendationAmount = 
                    (budget - monthlyExpensePerCategory - penaltyForUnexpectedExpensePerCategory) / restDaysOfMonth / 1000 * 1000;
            monthlyBudgetMap.put(categoryId, Math.max(todayRecommendationAmount, minimumDailyExpense));
        });
        return monthlyBudgetMap;
    }

    /**
     * 이번달 카테고리별 예산과 소비 지출 기록을 받아 예산이 설정되지 않은 카테고리에서의 소비 합계를 반환합니다.
     * @param monthlyBudget 이번달 설정한 카테고리별 예산
     * @param monthlyExpenses 이번달 소비한 카테고리별 지출 기록
     * @return 예산이 설정되지 않은 카테고리에서 소비한 금액의 합 반환
     */
    private long getExpenseForNotCreatedBudgetPlan(Map<Long, Long> monthlyBudget, Map<Long, Long> monthlyExpenses) {
        AtomicLong sum = new AtomicLong(0L);
        monthlyExpenses.forEach((categoryId, amount) -> {
            if (!monthlyBudget.containsKey(categoryId))
                sum.addAndGet(monthlyExpenses.get(categoryId));
        });
        return sum.get();
    }

    public Map<Long, Long> getTodayExpenses(long userId) {
        return expenseRepository.findTodayExpensesPerCategory(userId).stream()
                .collect(Collectors.toMap(map -> map.get("categoryId"), map -> map.get("amount")));
    }

    public List<CalendarStatDto> getExpenseMonthlyStat(long userId) {
        List<ExpenseProjection.SumCategory> lastMonthExpenses =
                expenseRepository.findMonthlyExpensesPerCategoryByYearAndMonthAndDayBefore(
                        userId, LocalDate.now().minusMonths(1));
        List<ExpenseProjection.SumCategory> thisMonthExpenses =
                expenseRepository.findMonthlyExpensesPerCategoryByYearAndMonthAndDayBefore(
                        userId, LocalDate.now());
        return getCalendarStatDtos(lastMonthExpenses, thisMonthExpenses);
    }

    public List<CalendarStatDto> getExpenseWeeklyStat(long userId) {
        List<ExpenseProjection.SumCategory> lastWeekSameDayExpenses =
                expenseRepository.findWeeklyExpensesPerCategoryByYearAndMonthAndDay(
                        userId, LocalDate.now().minusWeeks(1));
        List<ExpenseProjection.SumCategory> todayExpenses =
                expenseRepository.findWeeklyExpensesPerCategoryByYearAndMonthAndDay(
                        userId, LocalDate.now());
        return getCalendarStatDtos(lastWeekSameDayExpenses, todayExpenses);
    }

    private List<CalendarStatDto> getCalendarStatDtos(List<ExpenseProjection.SumCategory> lastExpenses,
                                                      List<ExpenseProjection.SumCategory> thisExpenses) {
        List<CalendarStatDto> result = new ArrayList<>();
        lastExpenses.forEach(lastExpense -> {
            AtomicLong thisExpense = new AtomicLong(0L);
            thisExpenses.stream()
                    .filter(sumCategory -> sumCategory.getCategoryId().equals(lastExpense.getCategoryId()))
                    .findFirst().ifPresent(sumCategory -> thisExpense.set(sumCategory.getAmount()));
            double expenseRate = (double) thisExpense.get() / lastExpense.getAmount() * 100;
            CalendarStatDto dto = new CalendarStatDto(
                    lastExpense.getCategoryId(),
                    BudgetCategory.values()[lastExpense.getCategoryId().intValue() - 1].getCategory(),
                    expenseRate);
            result.add(dto);
        });
        return result;
    }

    public Double getExpenseRateCompareOtherUserStat(long userId) {
        return expenseRepository.findExpenseRateCompareOtherUser(userId);
    }
}
