package com.limvik.econome.domain.category.enums;

import lombok.Getter;

/**
 * 통계청 소비지출 12대 비목을 기준으로 한 예산 항목 클래스입니다.
 */
@Getter
public enum BudgetCategory {

    GROCERIES("식료품/비주류음료"), // 곡물, 빵 및 떡류, 육류, 신선수산동물, 유제품 및 알, 과일 및 과일가공품, 채소 및 채소가공품, 과자, 기타 등등
    ALCOHOL_TOBACCO("주류/담배"),
    CLOTHING_FOOTWEAR("의류/신발"),
    HOUSING_UTILITIES("주거/수도/광열"), // 실제 주거비, 주택유지 및 수선, 상하수도 및 폐기물처리, 연료비
    HOUSEHOLD_GOODS_SERVICES("가정용품/가사서비스"), // 가구 및 조명, 가전/가정용 기기, 가사 소모품, 가사 서비스
    HEALTHCARE("보건"), // 의약품, 의료용소모품, 외래의료, 치과, 입원
    TRANSPORTATION("교통"), // 자동차 구입, 연료비, 항공/선박/기타여객 이용료
    COMMUNICATION("통신"), // 통신 장비, 통신 서비스
    ENTERTAINMENT("오락/문화"), // 운동 및 오락서비스, 문화 서비스, 국내외 단체/개인 여행
    EDUCATION("교육"), // 정규 교육, 학원/보습 교육
    FOOD_ACCOMMODATION("음식/숙박"), // 배달음식을 포함한 외식비, 숙박비
    OTHERS("기타 상품/서비스"); // 이미용 서비스, 위생 및 이미용 용품, 보험, 혼례 및 장제례비, 부동산 수수료 등등

    private final String category;

    BudgetCategory(String category) {
        this.category = category;
    }

}
