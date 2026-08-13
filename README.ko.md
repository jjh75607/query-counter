# query-counter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jjh75607/query-counter)](https://central.sonatype.com/artifact/io.github.jjh75607/query-counter)

[English](README.md)

***
> Spring 테스트에서 실행되는 쿼리의 수와 실행 시간을 검증하는 테스트 라이브러리

N+1 문제와 슬로우 쿼리를 배포 후가 아니라 테스트를 쓰는 단계에서 잡습니다.

#### 애노테이션 기반 도구로는 안 되는 것

- **테이블 단위 검증.** 테스트가 여러 테이블을 건드려도 관심 있는 테이블의 쿼리만 검증합니다.
- **런타임에 계산한 기대값.** 기대값이 `long`이라 `select(members.size() + 1)`이 그대로 됩니다.
  애노테이션은 값이 컴파일 시점에 고정되어야 하므로 테스트 데이터나 파라미터화 케이스에 따라
  달라지는 기대값을 표현할 수 없습니다.

#### 하지 않는 것

이건 좁은 도구입니다. [QuickPerf](https://github.com/quick-perf/quickperf)가 같은 영역을 훨씬
넓게 다루며, 아래는 QuickPerf에만 있습니다. N+1 직접 탐지, 조회하거나 수정한 컬럼 단위 검증,
JDBC 배치 검증, `LIKE '%...'`나 바인드 파라미터 없는 쿼리 같은 안티패턴 금지, JVM 프로파일링입니다.

테이블 단위 카운트나 런타임 계산 기대값을 플루언트 어서션으로 쓰고 싶으면 query-counter를,
넓은 범위가 필요하면 QuickPerf를 고르시면 됩니다.

#### 켜지 않으면 아무 일도 하지 않습니다

**활성화하지 않으면 이 라이브러리는 아무것도 하지 않습니다.** DataSource를 건드리지 않으므로
의존성을 추가하는 것만으로 이 라이브러리를 쓰지 않는 테스트의 동작이 바뀌는 일이 없습니다.
문서로만 약속한 것이 아니라 테스트로 고정해 두었습니다.

# 목차

- [시작하기](#시작하기)
    - [요구 사항](#요구-사항)
    - [설치](#설치)
    - [설정](#설정)
    - [사용법](#사용법)
- [API](#api)
    - [메서드](#메서드)
    - [예제](#예제)
- [검증 실패](#검증-실패)
    - [에러 메시지 형식](#에러-메시지-형식)
- [설정 항목](#설정-항목)

# 시작하기

## 요구 사항

| | 버전 |
|---|---|
| Java | 17 이상 |
| Spring Boot | 3.0 이상. 4.x 포함 |

변경마다 CI 가 Spring Boot 3.0.0, 3.5.x, 4.1.x 로 빌드하므로 위 범위는 짐작이 아니라
검증된 값입니다. Java 17 바이트코드를 목표로 하므로 그 이후 JVM 에서도 돕니다.

## 설치

`build.gradle`에 의존성을 추가해주세요. `mavenCentral()` 외에 더 필요한 것은 없습니다.

```groovy
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.jjh75607:query-counter:0.2.1")
}
```

Maven Central 은 `0.2.1` 부터 쓰실 수 있습니다. `0.2.0` 은 의존성 버전이 비어 있어 Gradle 로 해석되지 않습니다.
없습니다.

## 설정

테스트 설정에서 활성화합니다. 예를 들어 `src/test/resources/application.yml`입니다.

```yaml
query-counter:
  enabled: true
```

이 설정이 없으면 라이브러리가 동작하지 않고 쿼리도 기록되지 않습니다. 검증이 실패했는데 기록된
쿼리가 하나도 없으면 실패 메시지가 이 설정을 확인하라고 알려줍니다.

## 사용법

Spring 테스트의 `then` 블록에 검증을 씁니다. **애노테이션은 필요하지 않습니다.**

```java
@SpringBootTest
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
            .verify();           // 선택. 아래 설명 참고
    }
}
```

**`verify()`는 선택입니다.** 만들어두고 검증하지 않은 어서션은 테스트 메서드가 끝날 때 자동으로
검증되므로, `verify()`를 잊어서 테스트가 조용히 통과하는 일이 없습니다. 그 줄에서 바로 실패를
보고 싶을 때 호출하면 됩니다.

Spring 테스트 컨텍스트를 띄우지 않는 테스트라면 `@ExtendWith(QueryCountTestExtension.class)`를
붙여야 테스트 간에 기록이 격리됩니다. Spring 테스트에는 필요하지 않습니다.

### 돌아가는 예제

아래 예제들은 [`src/test/java/soon/springtestutil/example`](src/test/java/soon/springtestutil/example)
에 실제 테스트로도 들어 있어 `./gradlew build` 로 컴파일과 실행이 검증됩니다. 그 패키지는 JPA
엔티티를 써서 N+1 이 생기는 모습과 `join fetch` 로 사라지는 모습을 보여줍니다.

**셋업 쿼리도 카운트에 포함됩니다.** 기록은 테스트 메서드가 시작되기 직전에 초기화되므로
`@BeforeEach` 나 `given` 블록에서 실행한 쿼리가 함께 셉니다. `when` 블록의 쿼리만 세고 싶으면
셋업 뒤에 `QueryCountContext.clear()` 를 부르면 됩니다.

**JDBC 배치는 1건으로 셉니다.** `addBatch` 로 쌓고 `executeBatch` 로 보낸 문장은 몇 건을
쌓았든 1건입니다. 데이터베이스로 나가는 왕복이 한 번이기 때문입니다. `hibernate.jdbc.batch_size`
를 켠 프로젝트에서는 엔티티 10건을 저장해도 INSERT 카운트가 10이 아니라 1이 될 수 있습니다.
행 수가 아니라 왕복 횟수를 기대하시면 됩니다.

### API

##### 메서드

| 메서드 | 설명 |
|---|---|
| `forTable(String table)` | 특정 테이블에 대한 검증 조건을 설정합니다. 체이닝으로 테이블마다 다른 조건 설정 가능 |
| `forTables(String... tables)` | 지정한 테이블들의 쿼리를 합쳐서 검증합니다. 지정하지 않으면 모든 테이블을 검증합니다 |
| `forTables(List<String> tables)` | 리스트로 지정 |
| `select(long expected)` | SELECT 쿼리 실행 횟수 |
| `insert(long expected)` | INSERT 쿼리 실행 횟수 |
| `update(long expected)` | UPDATE 쿼리 실행 횟수 |
| `delete(long expected)` | DELETE 쿼리 실행 횟수 |
| `others(long expected)` | 나머지 쿼리 실행 횟수 |
| `maxExecutionTimeMs(long ms)` | 개별 쿼리가 이 시간을 넘기면 실패 |
| `verify()` | 지금 검증합니다. 선택 사항이며, 검증하지 않은 어서션은 테스트 후 자동으로 검사됩니다 |

기대값은 `long`이므로 상수뿐 아니라 식도 넘길 수 있습니다. 테스트 데이터의 개수나 파라미터화
테스트의 케이스에 따라 기대 쿼리 수가 달라지면 그대로 계산해서 지정하면 됩니다.

**상한 검증.** 카운트 메서드는 `ExpectedCount` 도 받습니다. 정확한 값이 아니라 상한을 검증할
수 있습니다. 구현 세부가 조금 바뀌어도 통과해야 하는 테스트가 여기 해당합니다.

```java
import static soon.springtestutil.querycount.assertion.ExpectedCount.atMost;

QueryCounterAssertion.assertCounts()
    .select(atMost(3))                       // 3개 이하면 통과
    .forTable("member").insert(atMost(1))    // 테이블별 검증에서도 됩니다
    .verify();
```

숫자를 그대로 넘기면 종전처럼 정확한 값 검증입니다. `select(3)` 과 `select(exactly(3))` 은
같습니다.

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
        .select(1)
        .insert(1)
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
        .forTables("orders", "products")
        .insert(2)
        .select(1)
        .update(1)
        .maxExecutionTimeMs(100)
        .verify();
}

@Test
void 테이블별로_다른_조건을_검증() {
    // given
    Member member = new Member("test");
    memberRepository.save(member);

    Product product = new Product("item", 1000);
    productRepository.save(product);

    // when
    memberService.getMember(member.getId());
    productService.getProducts();

    // then
    QueryCounterAssertion.assertCounts()
        .forTable("member").insert(1).select(1)
        .forTable("product").insert(1).select(1)
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

    // then
    QueryCounterAssertion.assertCounts()
        .forTables("member")
        .insert(count)
        .select(1)
        .verify();
}
```

### 검증 실패

- 예상과 실제 쿼리 횟수가 다르면 `AssertionError`가 발생합니다.
- `maxExecutionTimeMs`를 초과하는 쿼리가 있으면 `AssertionError`가 발생합니다.
- 어긋난 항목을 모두 모아 한 번에 보고하므로 하나씩 고쳐가며 발견하지 않아도 됩니다.

#### 에러 메시지 형식

```text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}] Query count assertion failed:
QueryType.SELECT: expected 3, but was 2
```

```text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}] Table-specific query count assertion failed:
Table 'member' - QueryType.SELECT: expected 2, but was 1
```

상한 검증도 같은 형식이고 비교 방식이 문구에 드러납니다.

```text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}] Query count assertion failed:
QueryType.SELECT: expected at most 2, but was 3
```

```text
java.lang.AssertionError: [Test: {패키지}.{클래스}#{메서드}]
Query execution time assertion failed: max=100ms, violations=1
[1] 120ms > 100ms, type=SELECT, SQL: SELECT * FROM member
```

검증이 실패했는데 기록된 쿼리가 하나도 없으면 안내가 덧붙습니다. 대개 설정을 빠뜨린 경우입니다.

```text
No query was recorded. Is query-counter.enabled=true set in your test configuration?
```

# 설정 항목

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `query-counter.enabled` | `false` | 쿼리 카운팅. 켜면 DataSource를 프록시로 감싸 실행된 쿼리를 기록합니다 |
| `query-counter.logging.enabled` | `false` | 실행된 SQL을 SLF4J로 출력합니다. 카운팅과 별개이며, 항상 켜져 있으면 테스트가 많은 프로젝트에서 로그가 오염되므로 기본값은 꺼짐입니다 |

```yaml
query-counter:
  enabled: true
  logging:
    enabled: false
```

두 프로퍼티 모두 IDE 자동완성에 설명과 함께 표시됩니다.
