# spring-test-util

[![JitPack](https://jitpack.io/v/jjh75607/spring-test-util.svg)](https://jitpack.io/#jjh75607/spring-test-util)

***
> Hibernate를 통해 실행되는 쿼리의 수와 실행 시간을 검증할 수 있는 테스트 라이브러리

#### 기대 효과

- N+1 문제를 테스트 단계에서 미리 발견
    - 쿼리 성능을 검증하는 테스트 코드 작성을 자연스럽게 유도
- 슬로우 쿼리 방지로 성능 최적화
- 특정 테이블만 선별적 검증 가능

# 목차

- [시작하기](#시작하기)
    - [설치](#설치)
    - [사용법](#사용법)
- [API](#api)
    - [메서드](#메서드)
    - [예제](#예제)
- [예외처리](#예외처리)
    - [에러 메시지 형식](#에러-메시지-형식)

# 시작하기

## 설치

`build.gradle`에 의존성을 추가해주세요.

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // 최신 버전은 상단의 JitPack 배지 참고
    testImplementation("com.github.jjh75607:spring-test-util:v0.0.5")
}
```

## 사용법

`@SpringBootTest`와 함께 테스트 코드에 `@ExtendWith(QueryCountTestExtension.class)` 어노테이션을 추가하여 사용합니다.

```java

@SpringBootTest
@ExtendWith(QueryCountTestExtension.class)
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Test
    void saveMember() {
        // given
        Member member = new Member("jjh75607", 12);

        // when
        memberService.createMember(member);

        // then
        QueryCounterAssertion.assertCounts()
            .forTables("member") // member 테이블에 대한 쿼리만 검증
            .insert(1)           // INSERT 1회 실행 예상
            .verify();           // 검증 실행
    }
}
```

### API

##### 메서드

| 메서드                              | 설명                                            |
  |----------------------------------|-----------------------------------------------|
| `forTables(String... tables)`    | 검증할 테이블 이름을 지정합니다. 지정하지 않으면 모든 테이블 쿼리를 검증합니다. |
| `forTables(List<String> tables)` | 리스트 형태로 검증할 테이블 이름 지정                         |
| `select(int expected)`           | SELECT 쿼리 실행 횟수 검증                            |
| `insert(int expected)`           | INSERT 쿼리 실행 횟수 검증                            |
| `update(int expected)`           | UPDATE 쿼리 실행 횟수 검증                            |
| `delete(int expected)`           | DELETE 쿼리 실행 횟수 검증                            |
| `others(int expected)`           | 나머지 쿼리 실행 횟수 검증                               |
| `maxExecutionTimeMs(long ms)`    | 개별 쿼리 최대 실행 시간을 지정, 초과 시 AssertionError 발생    |
| `verify()`                       | 설정한 조건으로 쿼리 검증 **반드시 호출**                     |

##### 예제

```java

@Test
void getMember() {
    // given
    Member member = new Member("test");
    memberRepository.save(member);

    // when
    memberService.getMember(member.getId());

    // then
    QueryCounterAssertion.assertCounts()
        .select(1)  // SELECT 1회
        .insert(1)  // INSERT 1회 (테스트 데이터 삽입)
        .verify();
}

@Test
void updateOrderWithExecutionTimeCheck() {
    // given
    Product product = new Product("item", 1000);
    productRepository.save(product);

    Order order = new Order(1L, product, 100);
    orderRepository.save(order);

    // when
    orderService.updateOrder(order.getId(), "item2", 200);

    // then
    QueryCounterAssertion.assertCounts()
        .forTables("orders", "products")  // 특정 테이블만 검증
        .insert(2)                        // orders, products 각각 INSERT 1회
        .select(1)                        // SELECT 1회
        .update(1)                        // UPDATE 1회
        .maxExecutionTimeMs(100)          // 개별 쿼리 최대 100ms
        .verify();
}

```

### 예외처리

- 예상과 실제 쿼리 횟수가 다르면 AssertionError 발생
- 예상 시간을 초과하는 쿼리가 있으면 AssertionError 발생

#### 에러 메시지 형식

``` text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}] Query count assertion failed:
QueryType.SELECT: expected 3, but was 2
```

``` text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}]
Query execution time assertion failed: max=100ms, violations=1
First violation: 120ms > 100ms, type=SELECT
SQL: SELECT * FROM member
```

#### 예시

```java

@Test
void failedVerify() {
    // given
    Member member = new Member("test");

    // when
    memberService.createMember(member); // INSERT 2회 발생

    // then
    QueryCounterAssertion.assertCounts()
        .insert(1) // 1회 예상
        .verify();  // AssertionError 발생
}

@Test
void failedExecutionTime() {
    // given
    jdbcTemplate.execute("CALL SLEEP(120)"); // 120ms 쿼리

    // then
    QueryCounterAssertion.assertCounts()
        .maxExecutionTimeMs(100) // 최대 100ms 지정
        .verify();               // AssertionError 발생
}

```