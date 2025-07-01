# spring-test-util

[![JitPack](https://jitpack.io/v/jjh75607/spring-test-util.svg)](https://jitpack.io/#jjh75607/spring-test-util)

***
> Hibernate를 통해 실행되는 쿼리의 수를 검증할 수 있는 테스트 라이브러리

# 목차

- [시작하기](#시작하기)
  - [설치](#설치)
  - [사용법](#사용법)
- [API](#api)
  - [메서드](#메서드)
  - [예제](#예제)
- [예외처리](#에러-메세지)
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
  // 최신 버전은 상단의 JitPack 뱃지를 참고
  testImplementation("com.github.jjh75607:spring-test-util:v0.0.3")
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
            .insert(1) // 명시하지 않은 다른 쿼리 유형은 검증 대상에서 제외됩니다.
            .verify();
  }
}
```

### API

##### 메서드

- `forTables(String... tables)`: 검증할 테이블 이름을 지정합니다. 지정하지 않으면 모든 테이블에 대한 쿼리를 검증합니다.
- `forTables(List<String> tables)`: 검증할 테이블 이름을 리스트로 지정합니다. 지정하지 않으면 모든 테이블에 대한 쿼리를 검증합니다.
- `select(int expected)`: `SELECT` 쿼리 실행 횟수를 검증합니다.
- `insert(int expected)`: `INSERT` 쿼리 실행 횟수를 검증합니다.
- `update(int expected)`: `UPDATE` 쿼리 실행 횟수를 검증합니다.
- `delete(int expected)`: `DELETE` 쿼리 실행 횟수를 검증합니다.
- `others(int expected)`: 위 네 가지 유형 이외의 쿼리 실행 횟수를 검증합니다.
- `verify()`: 설정한 쿼리 실행 횟수를 검증합니다. 이 메서드는 **반드시 호출**해야 합니다.

##### 예제

```java

@Test
void getMember() {
  // given
  Member member = new Member("test");
  memberRepository.save(member);

  // when
  memberService.getMember(member.getId());

  //then
  QueryCounterAssertion.assertCounts()
          .select(1) // SELECT 쿼리 1회 실행
          .insert(1) // INSERT 쿼리 1회 실행 (테스트 데이터 삽입)
          .verify(); // 검증 실행
}

@Test
void updateOrder() {
  // given
  Product product = new Product("item", 1000);
  productRepository.save(product);

  Order order = new Order(1L, product, 100);
  orderRepository.save(order);

  // when
  orderService.updateOrder(order.getId(), "item2", 200);

  // then
  QueryCounterAssertion.assertCounts()
          .forTables("orders", "products") // orders, products 테이블에 대한 쿼리 검증
          .insert(2) // orders, products 테이블에 각각 INSERT 쿼리 1회 실행
          .select(1) // orders 테이블에서 SELECT 쿼리 1회 실행
          .update(1) // orders 테이블에서 UPDATE 쿼리 1회 실행
          .verify();
}
```

### 예외처리

> 쿼리 실행 횟수가 예상과 다를 경우 `AssertionError`이 발생합니다.
> 예외 메시지에 어떤 쿼리가 몇 번 실행되었는지, 예상과 실제 실행 횟수의 차이를 포함합니다.

#### 에러 메시지 형식

java.lang.AssertionError: [Test: {패키지명}.{클래스명}#{메서드명}] Query count assertion failed:
QueryType.SELECT: expected 3, but was 2

#### 예시

```java

@Test
void failedVerify() {
  // given
  Member member = new Member("test");

  // when
  memberService.createMember(member); // 실제로는 INSERT 쿼리 2회 실행됨

  // then
  QueryCounterAssertion.assertCounts()
          .insert(1) // 1회 예상했지만 실제로는 2회 실행
          .verify(); // AssertionError 발생
}
```