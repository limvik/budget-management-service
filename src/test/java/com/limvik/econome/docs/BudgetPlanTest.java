package com.limvik.econome.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.budgetplan.BudgetPlanRepository;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListRequest;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListResponse;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanRequest;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanResponse;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.RequestHeadersSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@ExtendWith(RestDocumentationExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BudgetPlanTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    JwtConfig jwtConfig;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BudgetPlanRepository budgetPlanRepository;

    @LocalServerPort
    int port;

    RequestSpecification spec;

    String accessToken;

    User user;

    int year = 2023;
    int month = 5;
    final String BUDGET_PLAN_URL = "/api/v1/budget-plans?year=%d&month=%d".formatted(year, month);
    final String CATEGORIES_URL = "/api/v1/categories";

    long BUDGET_TOTAL_AMOUNT = 100000L;
    final String BUDGET_PLAN_RECOMMEND_URL = "/api/v1/budget-plans/recommendations?amount=%d".formatted(BUDGET_TOTAL_AMOUNT);

    final long BUDGET_PLAN_BASE_AMOUNT = 1000;

    @BeforeAll
    public void init() {
        user = User.builder()
                .username("budgetplans")
                .email("budgetplans@budget.com")
                .password("budgetplans")
                .minimumDailyExpense(10000L)
                .agreeAlarm(false)
                .build();
        user = userRepository.save(user);

        this.accessToken = jwtProvider.generateAccessToken(user);
    }

    @AfterEach
    public void tearDown() {
        budgetPlanRepository.deleteAllInBatch();
        budgetPlanRepository.flush();
    }

    @BeforeEach
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        this.spec = new RequestSpecBuilder()
                .addFilter(documentationConfiguration(restDocumentation).operationPreprocessors()
                        .withRequestDefaults(modifyUris().scheme("http").host("econome.com").removePort(), prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .setPort(this.port)
                .build();
    }

    @Test
    @DisplayName("예산 카테고리 목록 조회 성공")
    void shouldReturnCategoriesListWith200IfValidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-categories", getAccessTokenRequestHeaderSnippet(), getCategoriesResponseFieldsSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(CATEGORIES_URL)
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(stringContainsInOrder(getCategories()));
    }

    @Test
    @DisplayName("예산 카테고리 목록 조회 실패 - 유효하지 않은 Access Token")
    void shouldNotReturnCategoriesListWith401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-categories-unauthorized"))
                    .header("Authorization", "Bearer some.invalid.token")
                .when()
                    .get(CATEGORIES_URL)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    private List<String> getCategories() {
        return Arrays.stream(BudgetCategory.values())
                .map(BudgetCategory::getCategory)
                .toList();
    }

    private ResponseFieldsSnippet getCategoriesResponseFieldsSnippet() {
        return responseFields(
                beneathPath("categories"),
                fieldWithPath("id").description("카테고리 ID"),
                fieldWithPath("name").description("카테고리 이름")
        );
    }

    @Test
    @DisplayName("예산 설정 성공")
    void shouldCreateBudgetPlansWithReturn201IfValidToken() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-create", getAccessTokenRequestHeaderSnippet(), getBudgetPlanQueryParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getBudgetPlanListRequest(BUDGET_PLAN_BASE_AMOUNT)))
                .when()
                    .post(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(HttpStatus.CREATED.value()))
                    .body(emptyString());
    }

    @Test
    @DisplayName("예산 설정 실패 - 유효하지 않은 Access Token")
    void shouldNotCreateBudgetPlansWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-create-unauthorized"))
                    .header("Authorization", "Bearer some.invalid.token")
                .when()
                    .post(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    @Test
    @DisplayName("예산 설정 실패 - 중복된 예산")
    void shouldNotCreateBudgetPlansWithReturn409IfDuplicate() throws JsonProcessingException {
        shouldCreateBudgetPlansWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-create-conflict"))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getBudgetPlanListRequest(BUDGET_PLAN_BASE_AMOUNT)))
                .when()
                    .post(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(ErrorCode.DUPLICATED_BUDGET_PLAN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.DUPLICATED_BUDGET_PLAN.name()))
                    .body("errorReason", is(ErrorCode.DUPLICATED_BUDGET_PLAN.getMessage()));
    }

    @Test
    @DisplayName("예산 조회 성공")
    void shouldReturnBudgetPlansWith200IfValidToken() throws JsonProcessingException {
        shouldCreateBudgetPlansWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-get", getAccessTokenRequestHeaderSnippet(), getBudgetPlanQueryParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(containsString(objectMapper.writeValueAsString(getBudgetPlanListResponse())));
    }

    @Test
    @DisplayName("예산 조회 실패 - 유효하지 않은 Access Token")
    void shouldNotReturnBudgetPlansWith401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-get-unauthorized"))
                    .header("Authorization", "Bearer some.invalid.token")
                .when()
                    .get(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    private BudgetPlanListResponse getBudgetPlanListResponse() {
        List<BudgetPlanResponse> budgetPlanResponseList = new ArrayList<>();
        for (BudgetPlanRequest budgetPlan : getBudgetPlanListRequest(BUDGET_PLAN_BASE_AMOUNT).budgetPlans()) {
            BudgetPlanResponse budgetPlanResponse = new BudgetPlanResponse(
                    budgetPlan.categoryId(),
                    BudgetCategory.values()[(int)budgetPlan.categoryId()-1].getCategory(),
                    budgetPlan.amount());
            budgetPlanResponseList.add(budgetPlanResponse);
        }
        return new BudgetPlanListResponse(budgetPlanResponseList);
    }

    @Test
    @DisplayName("예산 수정 성공")
    void shouldUpdateBudgetPlansWith200IfValidToken() throws JsonProcessingException {
        shouldCreateBudgetPlansWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-update", getAccessTokenRequestHeaderSnippet(), getBudgetPlanQueryParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getBudgetPlanListRequest(BUDGET_PLAN_BASE_AMOUNT+1000)))
                .when()
                    .patch(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(emptyString());
    }

    @Test
    @DisplayName("예산 수정 실패 - 유효하지 않은 Access Token")
    void shouldNotUpdateBudgetPlansWith401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-update-unauthorized"))
                    .header("Authorization", "Bearer some.invalid.token")
                .when()
                    .patch(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    @Test
    @DisplayName("예산 수정 실패 - 존재하지 않는 예산")
    void shouldNotUpdateBudgetPlansWith404IfNotExist() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-update-not-found"))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getBudgetPlanListRequest(BUDGET_PLAN_BASE_AMOUNT+1000)))
                .when()
                    .patch(BUDGET_PLAN_URL)
                .then()
                    .statusCode(is(ErrorCode.NOT_EXIST_BUDGET_PLAN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.NOT_EXIST_BUDGET_PLAN.name()))
                    .body("errorReason", is(ErrorCode.NOT_EXIST_BUDGET_PLAN.getMessage()));
    }

    private BudgetPlanListRequest getBudgetPlanListRequest(long baseAmount) {
        List<BudgetPlanRequest> requests = new ArrayList<>();
        for (long i = 1; i <= BudgetCategory.values().length; i++) {
            requests.add(new BudgetPlanRequest(i, baseAmount * i));
        }
        return new BudgetPlanListRequest(requests);
    }

    private QueryParametersSnippet getBudgetPlanQueryParametersSnippet() {
        return queryParameters(
                parameterWithName("year")
                        .description("예산 연도"),
                parameterWithName("month")
                        .description("예산 월"));
    }

    @Test
    @DisplayName("예산 추천 성공")
    void shouldRecommendBudgetPlansWith200IfValidToken() throws JsonProcessingException {
        shouldCreateBudgetPlansWithReturn201IfValidToken();
        JsonPath body = RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-recommend", getAccessTokenRequestHeaderSnippet(), getRecommendBudgetPlanQueryParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(BUDGET_PLAN_RECOMMEND_URL)
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                .extract().body().jsonPath();
        assertThat(getAmountSumFromRecommendedBudget(body)).isEqualTo(BUDGET_TOTAL_AMOUNT);
    }

    private long getAmountSumFromRecommendedBudget(JsonPath recommendedBudget) {
        long amountSum = 0;
        for (int i = 0; i < BudgetCategory.values().length; i++) {
            amountSum += recommendedBudget.getLong("budgetPlans[" + i + "].amount");
        }
        return amountSum;
    }

    @Test
    @DisplayName("예산 추천 실패 - 유효하지 않은 Access Token")
    void shouldNotRecommendBudgetPlansWith401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("budget-plan-recommend-unauthorized"))
                    .header("Authorization", "Bearer some.invalid.token")
                .when()
                    .get(BUDGET_PLAN_RECOMMEND_URL)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    private QueryParametersSnippet getRecommendBudgetPlanQueryParametersSnippet() {
        return queryParameters(
                parameterWithName("amount")
                        .description("예산 금액"));
    }

    private RequestHeadersSnippet getAccessTokenRequestHeaderSnippet() {
        return requestHeaders(
                headerWithName("Authorization")
                        .description("Bearer JWT(Access Token)"));
    }

}
