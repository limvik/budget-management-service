package com.limvik.econome.domain.budgetplan.service;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.infrastructure.budgetplan.BudgetPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자의 카테고리별 예산 계획에 대한 서비스를 제공하는 클래스입니다.
 * 저장소에서 데이터를 불러와 도메인 객체를 호출하거나 도메인 객체에서 처리할 수 없는 비즈니스 로직을 처리합니다.
 * 그리고 처리된 데이터를 컨트롤러로 전달합니다.
 */
@RequiredArgsConstructor
@Service
public class BudgetPlanService {

    private final BudgetPlanRepository budgetPlanRepository;

    /**
     * 사용자가 지정한 카테고리별 예산 설정을 반영하여 저장소에 저장합니다.
     * @param budgetPlans 사용자가 예산 설정을 요청한 데이터
     * @throws ErrorException 사용자가 설정한 예산이 이미 존재하는 경우
     * @return 저장된 데이터를 반환합니다.
     */
    @Transactional
    public List<BudgetPlan> createBudgetPlans(List<BudgetPlan> budgetPlans) {
        if (hasExistPlan(budgetPlans)) {
            throw new ErrorException(ErrorCode.DUPLICATED_BUDGET_PLAN);
        }

        return budgetPlanRepository.saveAll(budgetPlans);
    }

    /**
     * 기존에 사용자가 설정해둔 예산의 금액을 사용자 요청에 의해 수정합니다.
     * @param budgetPlans 금액이 수정된 기존 카테고리별 예산
     * @throws ErrorException 사용자가 수정 요청한 예산이 존재하지 않는 경우
     */
    @Transactional
    public void updateBudgetPlans(List<BudgetPlan> budgetPlans) {
        if (hasNotExistPlan(budgetPlans)) {
            throw new ErrorException(ErrorCode.NOT_EXIST_BUDGET_PLAN);
        }
        budgetPlans.forEach(budgetPlan -> budgetPlanRepository.updateAmountByUserAndDateAndCategory(
                budgetPlan.getUser().getId(),
                budgetPlan.getCategory().getId(),
                budgetPlan.getDate(),
                budgetPlan.getAmount()));
    }

    private boolean hasExistPlan(List<BudgetPlan> budgetPlans) {
        return budgetPlanRepository.countByUserAndDateAndCategories(
                budgetPlans.get(0).getUser(), budgetPlans.get(0).getDate(),
                budgetPlans.stream().map(BudgetPlan::getCategory).toList()) != 0;
    }

    private boolean hasNotExistPlan(List<BudgetPlan> budgetPlans) {
        return budgetPlanRepository.countByUserAndDateAndCategories(
                budgetPlans.get(0).getUser(), budgetPlans.get(0).getDate(),
                budgetPlans.stream().map(BudgetPlan::getCategory).toList()) != budgetPlans.size();
    }

    /**
     * 사용자가 원하는 일자의 사용자 카테고리별 예산 목록을 반환합니다.
     * @param userId 사용자 식별자
     * @param date 사용자가 조회를 원하는 일자
     * @return 카테고리별 예산 목록 반환
     */
    @Transactional(readOnly = true)
    public List<BudgetPlan> getBudgetPlans(long userId, LocalDate date) {
        var user = User.builder().id(userId).build();
        return budgetPlanRepository.findAllByUserAndDate(user, date);
    }

    /**
     * 사용자의 전체 예산을 받아 카테고리별 추천 금액 목록을 반환합니다.
     * @param amount 사용자의 총 예산
     * @return 시스템이 추천하는 카테고리별 추천 금액 목록
     */
    @Transactional(readOnly = true)
    public List<BudgetPlan> getBudgetRecommendations(long amount) {
        List<BudgetPlan> budgetPlans = budgetPlanRepository.findRecommendedBudgetPlans(amount);
        long totalAmount = budgetPlans.stream().mapToLong(BudgetPlan::getAmount).sum();

        // 데이터베이스에서 불러올 때 소수점 연산으로 인해 총액과 차이나는 금액을 마지막 기타 카테고리에 반영
        budgetPlans.get(budgetPlans.size()-1).addAmount(amount - totalAmount);
        return budgetPlans;
    }

}
