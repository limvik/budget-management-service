# EconoMe

<p align="center">
  <img width="640" height="320" src="https://github.com/limvik/budget-management-service/assets/37972432/26624259-f239-477f-b230-79b227c2b081">
  <br><br>
  <img src="https://github.com/limvik/budget-management-service/actions/workflows/build-test-containerize-deploy.yml/badge.svg">
  <br><br>
</p>

## 개인 예산 관리 REST API

개인의 예산 관리 기능을 제공하는 REST API 입니다. 경제를 의미하는 Economy 에서 개인에 특화된 서비스임을 나타내기 위해 `EconoMe`로 이름을 지었습니다.

- 월 예산 추천: 사용자가 설정한 월 예산 총액을 바탕으로, 다른 사용자의 카테고리별 예산액 평균에 기반한 예산 추천
- 오늘의 추천 지출: 개인이 설정한 월 예산을 바탕으로, 오늘의 지출 금액 추천
- 오늘의 지출 조회: 오늘 예산 대비 사용자가 소비한 지출 금액 조회
- 통계: 지난 달/지난 주 대비 지출 비율, 다른 사용자 대비 지출 비율 조회

## 참고 사항

- 기간: 2023-11-09 ~ 2023-11-16(8일, 소요 시간 약 50 시간)
- 요구사항: https://bow-hair-db3.notion.site/90cba97a58a843e4a2563a226db3d5b5
- 프로젝트 관리: [Github Projects 링크](https://github.com/users/limvik/projects/2)
- [API 문서 링크](http://43.200.2.221/docs/index.html)

## 목차

- [Skills](#skills)
- [ERD](#erd)
- [배포 구조도](#배포-구조도)
- [API Table](#api-table)
- [구현 고려사항](#구현-고려사항)
  - [Refresh Token 클라이언트에 저장](#refresh-token-클라이언트에-저장)
  - [카테고리 항목 선정](#카테고리-항목-선정)
- [이슈 및 해결](#이슈-및-해결)
  - [TestRestTemplate 사용하여 401(Unauthorized) 수신 시 HttpRetryException 던짐](#testresttemplate-사용하여-401-수신-시-httpretryexception-던짐)
  - [Instant 와 LocalDate 및 LocalDateTime 혼용으로 인한 불일치](#instant-와-localdate-및-localdatetime-혼용으로-인한-불일치)
- [학습](#학습)
  - [Spring Security 흐름 다이어그램으로 정리](#spring-security-흐름-다이어그램으로-정리)
  - [블로그 학습 기록](#블로그-학습-기록)

## Skills

<p align="center">
  <img src="https://img.shields.io/badge/Java%2017-white?logo=openjdk&logoColor=black" alt="java">
  <img src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white" alt="gradle">
  <br><br>
  <img src="https://img.shields.io/badge/SpringBoot%203-6DB33F?logo=springboot&logoColor=white" alt="spring boot">
  <img src="https://img.shields.io/badge/SpringSecurity%206-6DB33F?logo=springsecurity&logoColor=white" alt="spring security">
  <img src="https://img.shields.io/badge/JWT-black?logo=JSON%20web%20tokens&logoColor=white" alt="JWT">
  <br><br>
  <img src="https://img.shields.io/badge/Spring%20Data%20JPA%20-6DB33F?logo=jpa&logoColor=white" alt="spring data jpa">
  <img src="https://img.shields.io/badge/MySql%208-4479A1?logo=mysql&logoColor=white" alt="mysql">
  <br><br>
  <img src="https://img.shields.io/badge/Spring%20REST%20Docs%20-6DB33F?logo=springrestdocs&logoColor=white" alt="spring rest docs">
  <br><br>
  <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="docker">
  <img src="https://img.shields.io/badge/Testcontainer-16d6c7?logo=linuxcontainers&logoColor=white" alt="testcontainer">
  <br><br>
  <img src="https://img.shields.io/badge/Postman-FF6C37?logo=postman&logoColor=white" alt="postman">
  <img src="https://img.shields.io/badge/IntelliJ%20Idea-000000?logo=intellijidea&logoColor=white" alt="intellij-idea">
</p>

<!-- ![java](https://img.shields.io/badge/Java%2017-white?logo=openjdk&logoColor=black)
커피 아이콘은 Oracle에서 삭제 요청 https://github.com/simple-icons/simple-icons/issues/7374 
![gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white)

![spring boot](https://img.shields.io/badge/SpringBoot%203-6DB33F?logo=springboot&logoColor=white)
![spring security](https://img.shields.io/badge/SpringSecurity%206-6DB33F?logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?logo=JSON%20web%20tokens&logoColor=white)

![spring data jpa](https://img.shields.io/badge/Spring%20Data%20JPA%20-6DB33F?logo=jpa&logoColor=white)
![mysql](https://img.shields.io/badge/MySql%208-4479A1?logo=mysql&logoColor=white)

![docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![testcontainer](https://img.shields.io/badge/Testcontainer-16d6c7?logo=linuxcontainers&logoColor=white)

![postman](https://img.shields.io/badge/Postman-FF6C37?logo=postman&logoColor=white)
![intellij-idea](https://img.shields.io/badge/IntelliJ%20Idea-000000?logo=intellijidea&logoColor=white)
-->

## ERD

<p align="center">
  <img width="598" height="480" src="https://github.com/limvik/budget-management-service/assets/37972432/6bda665f-4787-4e15-a788-500d3fbd0690">
</p>

- 카테고리(categories)는 예산 계획(budget_plans), 지출(expenses)과 1:M 관계가 있으므로, 관리를 위해 별도의 테이블을 추가하였습니다.
- 사용자(users) 테이블에서 설정 항목(minimum_daily_expenses, agree_alarm)과 refresh_token은 속성 상 다른 테이블에서 관리하는게 맞다고 판단되지만, 시간 제약이 있어 개발 속도 향상을 위해 통합하였습니다.
- 초기 수정 사항
  - 카카오 로그인 연동을 고려한 username 최대길이 변경: 12 -> 20
  - 간단하게 지출을 기록한다는 기획의도에 맞는 memo 길이 제한: TEXT -> VARCHAR(60)
  - create_time 처럼 과거분사를 사용하지 않도록 네이밍 컨벤션 통일
    - agreed_alarm -> agree_alarm
    - excluded_in_total -> exclude_in_total
- 기타
  - 돈을 저장하려면 나중에 원화 외의 다른 통화를 사용하는 것도 고려해서 DECIMAL을 사용하는 것에 대해 고민했는데, `기능을 예상해서 개발하지 말라`는 피드백을 받아서 BIGINT로 진행하였습니다.

[목차로 이동](#목차)

## 배포 구조도

![deployment](https://github.com/limvik/budget-management-service/assets/37972432/310f1ecc-9a1d-4990-8b38-21bc48ad214a)

[목차로 이동](#목차)

## API Table

### 사용자(Users)

|Operation|API Endpoint|HTTP Method|Response Status|Description|
|---|---|---|---|---|
|회원가입|/api/v1/users/signup|POST|201(Created)|회원가입 성공|
||||409(Conflict)|중복된 username 또는 email|
||||422(Unprocessable Content)|유효성 검사 실패|
|로그인|/api/v1/users/signin|POST|200(Ok)|로그인 성공|
||||401(Unauthorized)|인증되지 않음|
|AccessToken 재발급|/api/v1/users/token|POST|200(Ok)|accessToken 재발급 성공|
||||401(Unauthorized)|유효하지 않은 RefreshToken|

### 예산(Budget_Plans)

|Operation|API Endpoint|HTTP Method|Response Status|Description|
|---|---|---|---|---|
|예산 설정 카테고리 목록 조회|/api/v1/categories|GET|200(Ok)|예산 설정 카테고리 목록 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
|예산 설정|/api/v1/budget-plans?year=&month=|POST|201(Created)|예산 설정 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
||||409(conflict)|이미 설정된 예산|
|예산 조회|/api/v1/budget-plans?year=&month=|GET|200(Ok)|예산 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
|예산 수정|/api/v1/budget-plans?year=&month=|PATCH|200(Ok)|예산 수정 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
||||404(Not Found)|존재하지 않는 예산|
|예산 추천|/api/v1/budget-plans/recommendations?amount=|GET|200(Ok)|추천 예산 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|

[목차로 이동](#목차)

### 지출(Expenses)

|Operation|API Endpoint|HTTP Method|Response Status|Description|
|---|---|---|---|---|
|지출 생성|/api/v1/expenses|POST|201(Created)|지출 생성 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
|지출 수정|/api/v1/expenses/{id}|PATCH|200(Ok)|지출 수정 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
||||404(Not Found)|존재하지 않는 id|
|지출 조회|/api/v1/expenses/{id}|GET|200(Ok)|지출 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
||||404(Not Found)|존재하지 않는 id|
|지출 삭제|/api/v1/expenses/{id}|DELETE|204(No Content)|지출 삭제 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
||||404(Not Found)|존재하지 않는 id|
|지출 목록 조회|/api/v1/expenses?startDate={yyyy-mm-dd}&endDate={yyyy-mm-dd}&categoryId={categoryId}&minAmount={minAmount}&maxAmount={maxAmount}}|GET|200(Ok)|지출 목록 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|

### 추천/안내/통계

|Operation|API Endpoint|HTTP Method|Response Status|Description|
|---|---|---|---|---|
|오늘 지출 추천|/api/v1/expenses/recommendations|GET|200(Ok)|오늘 지출 추천 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
|오늘의 지출 안내|/api/v1/expenses/today|GET|200(Ok)|오늘의 지출 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|
|통계|/api/v1/expenses/stat|GET|200(Ok)|지난달/지난주 같은 요일/다른 사용자 대비 소비율 통계 정보 조회 성공|
||||401(Unauthorized)|유효하지 않은 AccessToken|

---

[목차로 이동](#목차)

## 구현 고려사항

구현 시 고려했던 내용을 일부 정리하였습니다.

### Refresh Token 클라이언트에 저장

Refresh Token을 클라이언트에 저장하는 것은 항상 탈취 가능성이 있으므로 보안적으로 위험하다고 생각해서 다른 방법을 찾아보았습니다. 아래와 같이 `BFF(Backend For Frontend) 패턴으로 세션과 토큰을 매핑`하는 방식이 있었습니다.

![image](https://github.com/limvik/budget-management-service/assets/37972432/93bf4516-acbf-4b50-a0c2-688746006726)  
출처: https://www.pingidentity.com/en/resources/blog/post/refresh-token-rotation-spa.html

`마감일까지 구현할 수 있을 가능성이 적어` 배운대로 Refresh Token과 Access Token을 로그인 시에 반환하고, Access Token 만료 시 Refresh Token으로 갱신된 Access Token을 발급받을 수 있도록 구현하였습니다.

대신 모든 엔드포인트가 사용자의 많은 정보를 필요로 하는것은 아니므로, 사용자의 식별자만 토큰에 저장하여 정보유출을 최소화했습니다.

---

### 카테고리 항목 선정

통계청 가계동향조사에 사용되는 소비지출 12대 비목을 기준으로 카테고리를 선정하였습니다. 통계청 자료에 가구 특성별, 소득별 소비지출 통계가 있어 사용자가 부족해도 데이터를 바탕으로 다양한 추천 서비스를 제공할 수 있을 것으로 기대합니다.

- 참고 자료
  - 통계청 가구특성별 비목별 소비지출([링크](https://kosis.kr/statHtml/statHtml.do?orgId=101&tblId=DT_1HDAB07))
  - 2023년 2/4분기 가계동향조사 결과([링크](https://eiec.kdi.re.kr/policy/materialView.do?num=241905))

---

[목차로 이동](#목차)

## 이슈 및 해결

### TestRestTemplate 사용하여 401 수신 시 HttpRetryException 던짐

#### 상황

슬라이스 테스트로 각각의 Controller, Service, Repository 에 대한 테스트를 구현하기에는 시간이 부족하여, TestRestTemplate을 이용한 통합 테스트를 구현하였습니다.

그런데 서버가 401(Unauthorized) 반환 시 body 에 데이터가 있는 경우, cannot retry due to server authentication, in streaming mode 오류가 발생하였습니다.

아래 코드에서 가장 아래있는 body에 데이터를 쓰는 코드(`response.getWriter().write(jsonResponse);`)만 제거하면 이상 없이 동작합니다.

```java
public class JwtEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setStatus(ErrorCode.INVALID_TOKEN.getHttpStatus().value());
        String jsonResponse = new ObjectMapper().writeValueAsString(
                new ErrorResponse(ErrorCode.INVALID_TOKEN.name(), ErrorCode.INVALID_TOKEN.getMessage()));
        response.getWriter().write(jsonResponse);

    }

}
```

#### 원인

TestRestTemplate은 HTTP 연결 시 Native Java API인 `HttpURLConnection`을 사용합니다. 기본적으로 Streaming Mode를 사용하여 HTTP 요청에 대한 응답을 버퍼에 저장하지 않고 즉시 처리합니다.

API 문서를 참조해보면, 아래와 같이 authentication 이나 redirection이 필요한 경우, 응답을 읽으려고 할 때 `HttpRetryException`이 던져짐을 언급하고 있습니다.

> A HttpRetryException will be thrown when reading the response if authentication or redirection are required.

401(Unauthorized)를 반환하는 경우 인증이 필요한 상황이고 response body에 접근하는 경우 HttpRetryException이 던져집니다.

- 참고자료
  - https://bugs.openjdk.org/browse/JDK-8118819?page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel&showAll=true
  - https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html#setFixedLengthStreamingMode-long-

#### 해결

HttpURLConnection을 직접 조작하는 방법도 있겠지만, HttpClient 라이브러리를 의존성에 추가만 하면 문제가 해결되었습니다.

```groovy
// For Spring Boot 3
testImplementation 'org.apache.httpcomponents.client5:httpclient5:5.2.1'

// For Spring Boot 2
testImplementation 'org.apache.httpcomponents:httpclient:4.5.14'
```

시간이 부족하고 Test에만 사용할 예정이므로 크게 고민할 필요가 없다고 판단했습니다. 그리고 TestRestTemplate으로 HTTP `PATCH` method를 지정하기 위해서는 필요했던 라이브러리기 때문에, 라이브러리를 추가하여 문제를 해결하였습니다.

---

[목차로 이동](#목차)

### Instant 와 LocalDate 및 LocalDateTime 혼용으로 인한 불일치

#### 상황

시간이 부족한 상황에서 평소 시간까지 표현하기 위해서 주로 사용하던 `Instant` 자료형을 사용하였습니다. DTO의 자료형이나 과거 테스트 데이터 생성 시 시간 조작 편의를 위해 `LocalDate`나 `LocalDateTime` 을 사용하였습니다.

데이터의 `날짜`를 기준으로 데이터베이스에서 불러올 때 예상했던 데이터를 가져오지 못하거나 더 가져오는 문제가 발생하였습니다.

#### 원인

`Instant`는 UTC+0인 기준시가 반환되고, `LocalDate`나 `LocalDateTime`은 시스템 TimeZone을 기반으로 시간을 반환합니다.

![image](https://github.com/limvik/budget-management-service/assets/37972432/5928b87c-b5d5-47c4-b134-fcd3a366b024)

#### 해결

`Instant`는 MySQL의 Timestamp 와 동일하게 Unix Timestamp이고, 글로벌 서비스를 마음 속에 품고(?) 개발을 하면서 `Instant`를 사용했습니다.

하지만 이번 프로젝트에서 데이터베이스를 설계하면서도 외화를 고려한 DECIMAL 타입을 고려할 때 너무 나갔다는 피드백을 받았고, 현재 요구사항에 충실하게 개발을 했습니다. 이에 맞춰서 `LocalDate`와 `LocalDateTime`을 사용하기로 결정하였습니다.

---

[목차로 이동](#목차)

## 학습

### Spring Security 흐름 다이어그램으로 정리

- Spring Security의 전체적인 그림을 이해하지 못해서 인증 절차에 시간이 많이 허비됐다고 생각하여, 전체적인 흐름을 살펴보고 다이어그램으로 그려보았습니다.

![spring-security-custom-authentication-filter-diagram](https://github.com/limvik/budget-management-service/assets/37972432/98f05a35-4b56-4f3d-b189-6c4b99ab5745)

[목차로 이동](#목차)

### 블로그 학습 기록

- [Character Sets와 Collations 차이](https://limvik.github.io/posts/what-is-the-diffrence-charset-and-collations-in-mysql/)
