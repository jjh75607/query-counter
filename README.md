# query-counter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jjh75607/query-counter)](https://central.sonatype.com/artifact/io.github.jjh75607/query-counter)

[한국어](README.ko.md)

***
> A test library for asserting how many queries a Spring test executes, and how long they take.

Catch N+1 problems and slow queries while writing tests, not after deploying.

#### What it does that annotation-based tools cannot

- **Assert per table.** Verify only the queries that touch the tables you care about, even when
  the test touches several.
- **Compute expected counts at runtime.** Expected values are plain `long` values, so
  `select(members.size() + 1)` works. Annotations need their values fixed at compile time,
  so they cannot express expectations that depend on test data or parameterized cases.
- **Catch N+1 without counting.** `noNPlusOne()` fails when the same SELECT ran with different
  parameter values, so you do not have to know how many rows the test data has.

#### What it does not do

This is a narrow tool. [QuickPerf](https://github.com/quick-perf/quickperf) covers a much wider
surface and does these, which query-counter does not: asserting on selected or updated columns,
asserting JDBC batching, forbidding anti-patterns such as `LIKE '%...'` or statements without
bind parameters, and JVM profiling.

Reach for query-counter when you want table-scoped counts or runtime-computed expectations with a
fluent assertion. Reach for QuickPerf when you want breadth.

#### Never in the way

**When you do not enable it, this library does nothing.** The DataSource is left untouched, so
adding the dependency cannot change the behaviour of tests that do not use it. That property is
locked in by a test, not just documented.

# Table of contents

- [Getting started](#getting-started)
    - [Requirements](#requirements)
    - [Install](#install)
    - [Configure](#configure)
    - [Usage](#usage)
- [API](#api)
    - [Methods](#methods)
    - [Examples](#examples)
- [Failures](#failures)
    - [Message format](#message-format)
- [Settings](#settings)

# Getting started

## Requirements

| | Version |
|---|---|
| Java | 17 or later |
| Spring Boot | 3.0 or later, including 4.x |

CI builds against Spring Boot 3.0.0, 3.5.x and 4.1.x on every change, so the range above is
verified rather than assumed. The library targets Java 17 bytecode, so it runs on any later JVM.

## Install

Add the dependency to `build.gradle`. Nothing beyond `mavenCentral()` is needed.

```groovy
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.jjh75607:query-counter:0.3.0")
}
```

Maven Central starts at `0.2.1`. Earlier releases either predate Central or cannot be resolved by Gradle.

## Configure

Enable it in your test configuration, for example `src/test/resources/application.yml`.

```yaml
query-counter:
  enabled: true
```

Without this, the library stays inactive and no query is recorded. If an assertion fails and
nothing was recorded, the failure message reminds you of this setting.

## Usage

Write the assertion in the `then` block of a Spring test. **No annotation is required.**

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
            .forTables("member") // only assert queries against member
            .insert(1)           // expect one INSERT
            .verify();           // optional, see below
    }
}
```

**`verify()` is optional.** An assertion that is created but never verified is checked
automatically when the test method finishes, so a forgotten `verify()` can never make a test pass
silently. Call it when you want the failure to surface at that exact line.

If your test does not load a Spring test context, add
`@ExtendWith(QueryCountTestExtension.class)` so that recorded queries are still isolated between
tests. Spring tests do not need it.

### Runnable examples

Every example below is also a real test in
[`src/test/java/soon/springtestutil/example`](src/test/java/soon/springtestutil/example),
so it compiles and runs with `./gradlew build`. That package uses JPA entities and shows an
N+1 problem appearing and then disappearing after a `join fetch`.

**Setup queries are counted too.** The recording is reset just before the test method starts, so
queries issued in `@BeforeEach` or in the `given` block are included. Call
`QueryCountContext.clear()` after your setup if you want to count only what the `when` block does.

**A JDBC batch counts as one.** Statements sent with `addBatch` and `executeBatch` are counted
once no matter how many were stacked, because that is one round trip to the database. If your
project enables `hibernate.jdbc.batch_size`, saving ten entities can produce a single INSERT
count rather than ten. Expect round trips, not rows.

### API

##### Methods

| Method | Description |
|---|---|
| `forTable(String table)` | Assert on a single table. Chain the call to assert different expectations per table |
| `forTables(String... tables)` | Restrict the assertion to the given tables, counted together. When omitted, every table is counted |
| `forTables(List<String> tables)` | Same, taking a list |
| `select(long expected)` | Expected number of SELECT statements |
| `insert(long expected)` | Expected number of INSERT statements |
| `update(long expected)` | Expected number of UPDATE statements |
| `delete(long expected)` | Expected number of DELETE statements |
| `others(long expected)` | Expected number of any other statements |
| `maxExecutionTimeMs(long ms)` | Fail if a single query takes longer than this |
| `noNPlusOne()` | Fail if the same SELECT ran with different parameter values |
| `verify()` | Run the assertions now. Optional, since unverified assertions are checked after the test |

Expected values are `long`, so expressions work as well as constants. When the expected number of
queries depends on the amount of test data or on a parameterized case, compute it.

**Upper bounds.** Every count method also accepts an `ExpectedCount`, so you can assert a ceiling
instead of an exact number. A test that should keep passing after a small implementation change is
usually of this kind.

```java
import static soon.springtestutil.querycount.assertion.ExpectedCount.atMost;

QueryCounterAssertion.assertCounts()
    .select(atMost(3))                       // three or fewer passes
    .forTable("member").insert(atMost(1))    // works per table too
    .verify();
```

A plain number stays an exact match, so `select(3)` and `select(exactly(3))` are the same.

**Catching N+1.** `noNPlusOne()` fails when one SELECT shape was executed more than once with
different parameter values. That is the shape of an N+1: one query per parent row.

```java
QueryCounterAssertion.assertCounts()
    .noNPlusOne()
    .verify();
```

You do not have to know how much test data there is, which `select(1 + members.size())` requires.

Repeating a SELECT with the **same** parameter values is not reported. That is a duplicate read,
not an N+1. A statement sent as a JDBC batch is one round trip, so it is not reported either.

There is no threshold: a single repetition with differing values fails. Use `select(atMost(n))`
when you want to allow a number of queries instead. When `forTables` is set, only queries against
those tables are considered.

##### Examples

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
void differentExpectationsPerTable() {
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
void expectedCountComputedAtRuntime() {
    // given - the expected number of queries follows the amount of data
    List<Member> members = memberRepository.saveAll(
        List.of(new Member("a"), new Member("b"), new Member("c"))
    );

    // when
    memberService.getMembersWithTeam();

    // then - one SELECT when teams are fetched together, members.size() + 1 when N+1 happens
    QueryCounterAssertion.assertCounts()
        .forTables("member")
        .select(1)
        .insert(members.size())
        .verify();
}

@ParameterizedTest
@ValueSource(ints = {1, 5, 10})
void expectationPerParameterizedCase(int count) {
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

### Failures

- An `AssertionError` is thrown when the number of queries differs from what you expected.
- An `AssertionError` is thrown when a query takes longer than `maxExecutionTimeMs`.
- An `AssertionError` is thrown when `noNPlusOne()` finds a repeated SELECT with differing parameters.
- Every mismatch is reported together, so you do not fix them one at a time.

#### Message format

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}] Query count assertion failed:
QueryType.SELECT: expected 3, but was 2
```

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}] Table-specific query count assertion failed:
Table 'member' - QueryType.SELECT: expected 2, but was 1
```

An upper bound reads the same way, with the comparison spelled out:

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}] Query count assertion failed:
QueryType.SELECT: expected at most 2, but was 3
```

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}]
Query execution time assertion failed: max=100ms, violations=1
[1] 120ms > 100ms, type=SELECT, SQL: SELECT * FROM member
```

An N+1 failure names the repeated statement and the values that differed, so you can see which
association to fetch:

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}]
N+1 assertion failed: 1 query shape ran with different parameter values
[1] 3 executions, 3 distinct parameter values
    SQL: select t1_0.id,t1_0.name from team t1_0 where t1_0.id=?
    params: [1], [2], [3]
```

When an assertion fails and no query was recorded at all, the message adds a reminder, because the
usual cause is a missing setting:

```text
No query was recorded. Is query-counter.enabled=true set in your test configuration?
```

# Settings

| Property | Default | Description |
|---|---|---|
| `query-counter.enabled` | `false` | Count queries. When enabled, the DataSource is wrapped in a proxy that records every executed query |
| `query-counter.logging.enabled` | `false` | Log every executed SQL statement through SLF4J. Independent of counting, and off by default because always-on SQL logging is noisy in projects with many tests |

```yaml
query-counter:
  enabled: true
  logging:
    enabled: false
```

Both properties appear with descriptions in IDE autocompletion.
