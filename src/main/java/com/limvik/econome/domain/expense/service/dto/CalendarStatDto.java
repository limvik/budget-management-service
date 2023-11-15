package com.limvik.econome.domain.expense.service.dto;

/**
 * 월간/주간 사용자 지출 통계정보 반환시 Service L에서 Controller로 데이터를 보내기 위한 DTO 입니다.
 * @param categoryId 카테고리 식별자
 * @param categoryName 카테고리 이름
 * @param expenseRate 지난 월/주 대비 지출 비율
 */
public record CalendarStatDto(
        Long categoryId,
        String categoryName,
        Double expenseRate
) {
}
