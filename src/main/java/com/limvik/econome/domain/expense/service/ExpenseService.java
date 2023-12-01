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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public void updateExpense(Expense updateExpense) {
        var expense = expenseRepository.findByUserAndId(updateExpense.getUser(), updateExpense.getId())
                .orElseThrow(() -> new ErrorException(ErrorCode.NOT_EXIST_EXPENSE));
        expense.update(updateExpense);
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
                LocalDateTime.parse(startInstant, DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.parse(postfixForEndInstant, DateTimeFormatter.ISO_DATE_TIME),
                minAmount, maxAmount);
    }

    @Transactional
    public void deleteExpense(long userId, long expenseId) {
        User user = User.builder().id(userId).build();
        Expense expense = expenseRepository.findByUserAndId(user, expenseId).orElseThrow(
                () -> new ErrorException(ErrorCode.NOT_EXIST_EXPENSE));
        expenseRepository.delete(expense);
    }

    /**
     * 사용자가 지정한 이번달 카테고리별 예산에서 이번달 어제까지의 카테고리별 소비 지출 금액을 제외한 후 남은 날짜만큼 예산을 균등분배한 후 반환합니다.
     * 만약 균등분배한 금액이 최소 일 소비액 보다 작다면, 최소 일 소비액을 반환합니다.
     * 예산이 설정되지 않은 소비 지출 금액은 전체 카테고리에 균등 분배하여 차감합니다.
     * @param userId 사용자 id
     * @return 예산이 설정된 카테고리별 오늘의 추천 금액 반환
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getTodayRecommendationExpenses(long userId) {

        Map<Long, Long> monthlyBudgetMap = getThisMonthBudgetPlans(userId);
        Map<Long, Long> monthlyExpensesMap = getThisMonthExpensesBeforeToday(userId);

        return calculateRecommendations(monthlyBudgetMap, monthlyExpensesMap, userId);
    }

    private Map<Long, Long> getThisMonthBudgetPlans(long userId) {
        return budgetPlanRepository.findThisMonthBudgetPerCategory(userId, LocalDate.now())
                .get().collect(Collectors.toMap(budgetPlan -> budgetPlan.getCategoryId(), budgetPlan -> budgetPlan.getAmount()));
    }

    private Map<Long, Long> getThisMonthExpensesBeforeToday(long userId) {
        return expenseRepository.findThisMonthExpensesPerCategory(userId, LocalDate.now())
                .get().collect(Collectors.toMap(expense -> expense.getCategoryId(), expense -> expense.getAmount()));
    }

    private Map<Long, Long> calculateRecommendations(Map<Long, Long> monthlyBudgetMap,
                                                     Map<Long, Long> monthlyExpensesMap,
                                                     long userId) {
        int restDaysOfMonth = LocalDate.now().lengthOfMonth() - LocalDate.now().getDayOfMonth() + 1;
        long minimumDailyExpense = userRepository.findMinimumDailyExpenseById(userId);
        long expenseForNotCreatedBudgetPlan = getExpenseForNotCreatedBudgetPlan(monthlyBudgetMap, monthlyExpensesMap);
        long penaltyForUnexpectedExpensePerCategory = expenseForNotCreatedBudgetPlan / monthlyBudgetMap.size();
        monthlyBudgetMap.forEach((categoryId, budget) -> {
            long monthlyExpensePerCategory = monthlyExpensesMap.getOrDefault(categoryId, 0L);
            long todayRecommendationAmount =
                    (budget - monthlyExpensePerCategory - penaltyForUnexpectedExpensePerCategory) / restDaysOfMonth;
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

    @Transactional(readOnly = true)
    public Map<Long, Long> getTodayExpenses(long userId) {
        return expenseRepository.findTodayExpensesPerCategory(userId).
                get().collect(Collectors.toMap(expense -> expense.getCategoryId(), expense -> expense.getAmount()));
    }

    @Transactional(readOnly = true)
    public List<CalendarStatDto> getExpenseMonthlyStat(long userId) {
        List<ExpenseProjection.SumCategory> lastMonthExpenses =
                expenseRepository.findMonthlyExpensesPerCategoryByYearAndMonthAndDayBefore(
                        userId, LocalDate.now().minusMonths(1));
        List<ExpenseProjection.SumCategory> thisMonthExpenses =
                expenseRepository.findMonthlyExpensesPerCategoryByYearAndMonthAndDayBefore(
                        userId, LocalDate.now());
        return getCalendarStatDtos(lastMonthExpenses, thisMonthExpenses);
    }

    @Transactional(readOnly = true)
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
        thisExpenses.forEach(thisExpense -> {
            AtomicLong lastExpense = new AtomicLong(0L);
            lastExpenses.stream()
                    .filter(sumCategory -> sumCategory.getCategoryId().equals(thisExpense.getCategoryId()))
                    .findFirst().ifPresent(sumCategory -> lastExpense.set(sumCategory.getAmount()));

            result.add(getCalendarStatDto(thisExpense, lastExpense));
        });
        return result;
    }

    private CalendarStatDto getCalendarStatDto(ExpenseProjection.SumCategory thisExpense,
                                               AtomicLong lastExpense) {
        return new CalendarStatDto(
                thisExpense.getCategoryId(),
                BudgetCategory.values()[thisExpense.getCategoryId().intValue() - 1].getCategory(),
                getExpenseRateCompareLastExpense(thisExpense, lastExpense));
    }

    private double getExpenseRateCompareLastExpense(ExpenseProjection.SumCategory thisExpense,
                                                    AtomicLong lastExpense) {
        if (lastExpense.get() == 0L) {
            return -1.0;
        } else if (thisExpense.getAmount() == null || thisExpense.getAmount() == 0L) {
            return 0.0;
        } else {
            return (double) thisExpense.getAmount() / lastExpense.get() * 100;
        }
    }

    @Transactional(readOnly = true)
    public Double getExpenseRateCompareOtherUserStat(long userId) {
        return expenseRepository.findExpenseRateCompareOtherUser(userId);
    }
}
