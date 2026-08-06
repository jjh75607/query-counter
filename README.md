# query-counter

[![JitPack](https://jitpack.io/v/jjh75607/query-counter.svg)](https://jitpack.io/#jjh75607/query-counter)

***
> Hibernate를 통해 실행되는 쿼리의 수와 실행 시간을 검증할 수 있는 테스트 라이브러리

#### 기대 효과

- N+1 문제를 테스트 단계에서 미리 발견
    - 쿼리 성능을 검증하는 테스트 코드 작성을 자연스럽게 유도
- 슬로우 쿼리 방지로 성능 최적화
- 특정 테이블만 선별적 검증 가능
- 기대 쿼리 수를 코드로 계산해서 지정 가능
    - `select(members.size() + 1)` 처럼 테스트 데이터에 따라 달라지는 기대값을 그대로 표현
    - 애노테이션 기반 도구는 기대값이 컴파일 시점에 고정되어야 하므로 표현할 수 없는 경우

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
    testImplementation("com.github.jjh75607:query-counter:v0.0.6")
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
| `forTable(String table)`         | 특정 테이블에 대한 쿼리 검증 조건을 개별 설정합니다. 체이닝으로 여러 테이블에 각각 다른 조건 설정 가능 |
| `forTables(String... tables)`    | 검증할 테이블 이름을 지정합니다. 지정하지 않으면 모든 테이블 쿼리를 검증합니다. |
| `forTables(List<String> tables)` | 리스트 형태로 검증할 테이블 이름 지정                         |
| `select(long expected)`          | SELECT 쿼리 실행 횟수 검증                            |
| `insert(long expected)`          | INSERT 쿼리 실행 횟수 검증                            |
| `update(long expected)`          | UPDATE 쿼리 실행 횟수 검증                            |
| `delete(long expected)`          | DELETE 쿼리 실행 횟수 검증                            |
| `others(long expected)`          | 나머지 쿼리 실행 횟수 검증                               |
| `maxExecutionTimeMs(long ms)`    | 개별 쿼리 최대 실행 시간을 지정, 초과 시 AssertionError 발생    |
| `verify()`                       | 설정한 조건으로 쿼리 검증 **반드시 호출**                     |

기대값은 `long`이므로 상수뿐 아니라 식도 넘길 수 있습니다. 테스트 데이터의 개수나 파라미터화 테스트의 케이스에 따라 기대 쿼리 수가 달라지는 경우 그대로 계산해서 지정하면 됩니다.

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

@Test
void verifyQueryCountPerTable() {
    // given
    Member member = new Member("test");
    memberRepository.save(member);

    Product product = new Product("item", 1000);
    productRepository.save(product);

    // when
    memberService.getMember(member.getId());
    productService.getProducts();

    // then - 테이블별로 각각 다른 쿼리 수 검증
    QueryCounterAssertion.assertCounts()
        .forTable("member").insert(1).select(1)
        .forTable("product").insert(1).select(1)
        .verify();
}

@Test
void verifyQueryCountPerTableWithExecutionTime() {
    // given
    Member member = new Member("test");
    memberRepository.save(member);

    // when
    memberService.getMember(member.getId());

    // then - 테이블별 쿼리 수 + 실행 시간 검증
    QueryCounterAssertion.assertCounts()
        .forTable("member").insert(1).select(1).maxExecutionTimeMs(100)
        .forTable("orders").select(2).maxExecutionTimeMs(200)
        .verify();
}

@Test
void 기대값을_런타임에_계산() {
    // given - 데이터 개수가 바뀌면 기대 쿼리 수도 함께 바뀐다
    List<Member> members = memberRepository.saveAll(
        List.of(new Member("a"), new Member("b"), new Member("c"))
    );

    // when
    memberService.getMembersWithTeam();

    // then - 팀을 한 번에 가져오면 SELECT 1회, N+1이면 members.size() + 1회
    QueryCounterAssertion.assertCounts()
        .forTables("member")
        .select(1)
        .insert(members.size())
        .verify();
}

@ParameterizedTest
@ValueSource(ints = {1, 5, 10})
void 케이스마다_기대값이_다른_경우(int count) {
    // given
    for (int i = 0; i < count; i++) {
        memberRepository.save(new Member("member" + i));
    }

    // when
    memberService.getMembers();

    // then - 케이스별 기대값을 파라미터로 계산
    QueryCounterAssertion.assertCounts()
        .forTables("member")
        .insert(count)
        .select(1)
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
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}] Table-specific query count assertion failed:
Table 'member' - QueryType.SELECT: expected 2, but was 1
```

``` text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}]
Query execution time assertion failed: max=100ms, violations=1
First violation: 120ms > 100ms, type=SELECT
SQL: SELECT * FROM member
```

``` text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}]
Table 'member' execution time assertion failed: max=100ms, violations=1
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