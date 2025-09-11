package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(QueryCountTestExtension.class)
class QueryCounterAssertionTest {

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    @DisplayName("쿼리 횟수가 예상과 일치하면 예외를 발생하지 않는다.")
    @Test
    void verifyShouldPassWhenCountMatch() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");

        // expected
        QueryCounterAssertion.assertCounts()
            .forTables("member")
            .select(2)
            .insert(1)
            .verify();
    }

    @DisplayName("쿼리 횟수가 예상과 일치하지 않는다면 예외가 발생한다.")
    @Test
    void verifyShouldFailWhenCountsDoNotMatch() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(2)
            .insert(1)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.SELECT: expected 2, but was 1");
    }

    @DisplayName("지정하지 않은 쿼리 타입은 검증하지 않는다.")
    @Test
    void verifyShouldIgnoreUnspecifiedTypes() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (2, 'test2')");
        QueryCountContext.addQuery(QueryType.UPDATE, "UPDATE member SET name = 'test' WHERE id = 1");

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

    @DisplayName("여러 타입 중 일부만 지정하면, 지정한 타입만 검증한다.")
    @Test
    void verifyShouldCheckOnlySpecifiedTypes() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (2, 'test2')");
        QueryCountContext.addQuery(QueryType.UPDATE, "UPDATE member SET name = 'test' WHERE id = 1");

        // expected
        QueryCounterAssertion.assertCounts()
            .insert(2)
            .verify();
    }

    @Test
    @DisplayName("지정하지 않은 타입은 무시하고, 지정한 타입만 검증한다.")
    void verifyShouldOnlyCheckSpecifiedType() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .insert(1)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.INSERT: expected 1, but was 0");
    }

    @Test
    @DisplayName("forTables로 지정된 테이블의 쿼리만 검증한다.")
    void verifyShouldCheckOnlySpecifiedTables() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM orders");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");

        // expected
        QueryCounterAssertion.assertCounts()
            .forTables("member")
            .select(1)
            .insert(1)
            .verify();
    }

    @Test
    @DisplayName("forTables로 지정된 테이블의 쿼리만 검증한다.")
    void verifyShouldCheckOnlySpecifiedTablesWithList() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM product");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT m.name, o.order_date FROM member m JOIN orders o ON m.id = o.member_id");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");
        QueryCountContext.addQuery(QueryType.UPDATE, "UPDATE product SET name = 'test' WHERE id = 1");

        // expected
        QueryCounterAssertion.assertCounts()
            .forTables(List.of("member", "orders"))
            .select(2)
            .insert(1)
            .verify();
    }

    @Test
    @DisplayName("forTables로 지정된 테이블의 쿼리 횟수가 다르면 예외가 발생한다.")
    void verifyShouldFailWhenCountsDoNotMatchForSpecifiedTables() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM orders");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .forTables("member")
            .select(2)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.SELECT: expected 2, but was 1");
    }

    @Test
    @DisplayName("쿼리 실행 시간이 maxExecutionTimeMs 이하이면 통과한다.")
    void verifyShouldPassWhenExecutionTimeWithinLimit() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member", 50L);
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')", 80L);

        // expected
        QueryCounterAssertion.assertCounts()
            .maxExecutionTimeMs(100)
            .select(1)
            .insert(1)
            .verify();
    }

    @Test
    @DisplayName("쿼리 실행 시간이 maxExecutionTimeMs를 초과하면 예외가 발생한다.")
    void verifyShouldFailWhenExecutionTimeExceedsLimit() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member", 150L);

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .maxExecutionTimeMs(100)
            .select(1)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Query execution time assertion failed");
    }

    @Test
    @DisplayName("실행 시간이 null인 쿼리는 maxExecutionTimeMs 검증에서 무시된다.")
    void verifyShouldIgnoreQueriesWithoutExecutionTime() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')", 50L);

        // expected
        QueryCounterAssertion.assertCounts()
            .maxExecutionTimeMs(100)
            .select(1)
            .insert(1)
            .verify();
    }

    @Test
    @DisplayName("forTables로 여러 테이블을 지정하면 해당 모든 테이블의 쿼리가 실행 시간/카운트 검증에 포함된다.")
    void verifyShouldIncludeExecutionTimeFromAllSpecifiedTables() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member", 50L);
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM orders", 60L);
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT m.name, o.id FROM member m JOIN orders o ON m.id = o.member_id", 90L);
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM product", 40L); // 필터에서 제외

        // expected
        QueryCounterAssertion.assertCounts()
            .forTables("member", "orders")
            .maxExecutionTimeMs(100)
            .select(3)
            .verify();
    }

}