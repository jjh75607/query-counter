# query-counter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jjh75607/query-counter)](https://central.sonatype.com/artifact/io.github.jjh75607/query-counter)
[![CI](https://github.com/jjh75607/query-counter/actions/workflows/ci.yml/badge.svg)](https://github.com/jjh75607/query-counter/actions/workflows/ci.yml)

[한국어](README.ko.md)

***
> **Keeps a fixed N+1 from coming back.**

You fixed the N+1 with a `join fetch`. Six months later someone adds a field to the DTO, changes a
repository method, or touches a lazy field from a new screen. **Nobody re-counts the queries in
that pull request.** The N+1 is back.

This library turns that into a failing test. It is a chain in the `then` block, with no annotation
on the test and no setup code inside it.

```java
QueryCounterAssertion.assertCounts()
    .forTables("member")
    .select(1)
    .noNPlusOne()
    .verify();
```

## What it prevents

- **A fixed N+1 coming back.** `noNPlusOne()` fails when the same SELECT runs with different
  parameter values. You do not need to know how much test data there is.
- **A query count creeping up unnoticed.** `select(3)` fails the build the moment it becomes 5,
  and lists the statements that ran.
- **A slow query slipping in.** `maxExecutionTimeMs(100)` fails when a single statement crosses it.
- **This library breaking other people's tests.** It leaves the DataSource untouched until you
  enable it. That is locked in by a test, not just documented.

## How it differs from other tools

| Library | How an assertion is written | What the test sets up |
|---|---|---|
| query-counter | A chain in the `then` block, taking `long` values that can be computed | One line of yml. The DataSource is wrapped for you |
| [QuickPerf](https://github.com/quick-perf/quickperf) | An annotation on the test method, such as `@ExpectSelect(1)` | Framework specific wiring, covered by its own guides |
| [datasource-assert](https://github.com/ttddyy/datasource-assert) | Assertions over the recorded executions of a `ProxyTestDataSource` | The proxy DataSource, constructed in the test |

Two differences show up in daily use. An expected count that follows the amount of test data or a
parameterized case is an expression here rather than a constant, and an annotation cannot hold an
expression. And nothing is recorded until `query-counter.enabled=true` is set, so adding the
dependency cannot change how existing tests behave.

QuickPerf reaches well beyond SQL, into JVM heap measurement among other things, so pick it up when
you want that range. The N+1 rule below follows how QuickPerf defines an N+1.

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

Maven Central starts at `0.2.1`. `0.2.0` is on Central but its published dependency versions are
empty, so Gradle cannot resolve it, and anything earlier was never published there.

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

### What gets counted

**Setup queries are counted too.** The recording is reset just before the test method starts, so
queries issued in `@BeforeEach` or in the `given` block are included. Call
`QueryCountContext.clear()` after your setup if you want to count only what the `when` block does.

**A JDBC batch counts as one.** Statements sent with `addBatch` and `executeBatch` are counted
once no matter how many were stacked, because that is one round trip to the database. If your
project enables `hibernate.jdbc.batch_size`, saving ten entities can produce a single INSERT
count rather than ten. Expect round trips, not rows.

## API

### Methods

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

### Examples

Each example below is a real test in
[`src/test/java/soon/springtestutil/example/QueryCounterExampleTest.java`](src/test/java/soon/springtestutil/example/QueryCounterExampleTest.java),
so it compiles and runs with `./gradlew build`. Only the display names and the comment language
are adapted here. That package uses JPA entities and shows an N+1 appearing and then disappearing
after a `join fetch`.

`saveMembersInSeparateTeams(n)` saves n members, each in a team of its own, then flushes and clears
the persistence context. Every member needs a different team for an N+1 to be visible at all, since
a shared team would be read once and then served from the persistence context.

```java
@Test
void countByQueryType() {
    // given - one team and one member are saved, so two INSERTs
    saveMembersInSeparateTeams(1);

    // when
    memberRepository.findAllLazily();

    // then
    QueryCounterAssertion.assertCounts()
        .insert(2)
        .select(1)
        .verify();
}

@Test
void noNPlusOnePassesWithJoinFetch() {
    // given
    saveMembersInSeparateTeams(3);
    QueryCountContext.clear();

    // when
    memberRepository.findAllWithTeam().forEach(member -> member.getTeam().getName());

    // then
    QueryCounterAssertion.assertCounts()
        .noNPlusOne()
        .verify();
}

@Test
void differentExpectationsPerTable() {
    // given
    saveMembersInSeparateTeams(2);
    QueryCountContext.clear();

    // when
    memberRepository.findAllLazily().forEach(member -> member.getTeam().getName());

    // then - members are read at once, and a team is read once per member
    QueryCounterAssertion.assertCounts()
        .forTable("member").select(1)
        .forTable("team").select(2)
        .verify();
}

@Test
void assertExecutionTimeAlongWithCounts() {
    // given
    saveMembersInSeparateTeams(1);
    QueryCountContext.clear();

    // when
    memberRepository.findAllWithTeam();

    // then - a single statement over the limit raises an AssertionError.
    // On in-memory H2 every query stays well under it.
    QueryCounterAssertion.assertCounts()
        .select(1)
        .maxExecutionTimeMs(1000)
        .verify();
}

@ParameterizedTest
@ValueSource(ints = {1, 3, 5})
void expectationPerParameterizedCase(int memberCount) {
    // given
    saveMembersInSeparateTeams(memberCount);
    QueryCountContext.clear();

    // when
    memberRepository.findAllLazily().forEach(member -> member.getTeam().getName());

    // then - an annotation cannot express this
    QueryCounterAssertion.assertCounts()
        .select(1 + memberCount)
        .verify();
}
```

## Failures

- An `AssertionError` is thrown when the number of queries differs from what you expected.
- An `AssertionError` is thrown when a query takes longer than `maxExecutionTimeMs`.
- An `AssertionError` is thrown when `noNPlusOne()` finds a repeated SELECT with differing parameters.
- Every mismatch is reported together, so you do not fix them one at a time.

### Message format

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}] Query count assertion failed:
QueryType.SELECT: expected 1, but was 4
  [1] select m1_0.id,m1_0.name,m1_0.team_id from member m1_0
  [2] select t1_0.id,t1_0.name from team t1_0 where t1_0.id=?
  [3] select t1_0.id,t1_0.name from team t1_0 where t1_0.id=?
  ... and 1 more
```

The statements that were counted are listed, so you do not have to turn on SQL logging and run the
test again to find out which ones they were. At most three are shown.

```text
java.lang.AssertionError: [Test: {package}.{class}#{method}] Table-specific query count assertion failed:
Table 'member' - QueryType.SELECT: expected 2, but was 1
  [1] select m1_0.id,m1_0.name from member m1_0
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

## Settings

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

## Changelog

What changed in each release is in [CHANGELOG.md](CHANGELOG.md).
