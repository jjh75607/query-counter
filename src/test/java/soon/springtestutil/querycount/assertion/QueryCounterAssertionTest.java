package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        // expected - SELECT만 지정하면 INSERT, UPDATE는 검증하지 않음
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();

        // INSERT만 지정해도 동일하게 동작
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (2, 'test2')");

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

    @Test
    @DisplayName("forTable 검증에서 쿼리 횟수가 일치하지 않으면 예외가 발생한다.")
    void forTableShouldFailWhenCountsDoNotMatch() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO product (id, name) VALUES (1, 'test')");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .forTable("member").select(2)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Table 'member' - QueryType.SELECT: expected 2, but was 1");
    }


    @Test
    @DisplayName("forTable과 forTables를 함께 사용할 수 있다.")
    void forTableAndForTablesShouldWorkTogether() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM orders");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO product (id, name) VALUES (1, 'test')");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO product (id, name) VALUES (2, 'test2')");

        // expected - forTables로 member, orders 합계 검증 + forTable로 product 개별 검증
        QueryCounterAssertion.assertCounts()
            .forTables("member", "orders")
            .select(2)
            .forTable("product").insert(2)
            .verify();
    }


    @Test
    @DisplayName("UPDATE 쿼리 횟수가 예상과 다르면 예외가 발생한다.")
    void verifyShouldFailWhenUpdateCountDoesNotMatch() {
        // given
        QueryCountContext.addQuery(QueryType.UPDATE, "UPDATE member SET name = 'test' WHERE id = 1");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .update(2)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.UPDATE: expected 2, but was 1");
    }


    @Test
    @DisplayName("DELETE 쿼리 횟수가 예상과 다르면 예외가 발생한다.")
    void verifyShouldFailWhenDeleteCountDoesNotMatch() {
        // given
        QueryCountContext.addQuery(QueryType.DELETE, "DELETE FROM member WHERE id = 1");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .delete(2)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.DELETE: expected 2, but was 1");
    }


    @Test
    @DisplayName("OTHERS 쿼리 횟수가 예상과 다르면 예외가 발생한다.")
    void verifyShouldFailWhenOthersCountDoesNotMatch() {
        // given
        QueryCountContext.addQuery(QueryType.OTHERS, "CREATE TABLE test (id INT)");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .others(2)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.OTHERS: expected 2, but was 1");
    }

    @Test
    @DisplayName("아무 조건도 설정하지 않으면 검증 없이 통과한다.")
    void verifyShouldPassWithNoConditions() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id) VALUES (1)");

        // expected
        QueryCounterAssertion.assertCounts().verify();
    }

    @Test
    @DisplayName("쿼리가 없을 때 0 카운트 검증이 통과한다.")
    void verifyShouldPassWithZeroCountWhenNoQueries() {
        // expected
        QueryCounterAssertion.assertCounts()
            .select(0)
            .insert(0)
            .update(0)
            .delete(0)
            .verify();
    }

    @Test
    @DisplayName("여러 쿼리 타입의 카운트 불일치 시 모든 오류가 메시지에 포함된다.")
    void verifyShouldIncludeAllMismatchesInErrorMessage() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id) VALUES (1)");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(2)
            .insert(2)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.SELECT: expected 2, but was 1")
            .hasMessageContaining("QueryType.INSERT: expected 2, but was 1");
    }

    @Test
    @DisplayName("verify 후 QueryCountContext가 초기화된다.")
    void verifyShouldClearContextAfterExecution() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");

        // when
        QueryCounterAssertion.assertCounts().select(1).verify();

        // then
        assertThat(QueryCountContext.getQueries()).isEmpty();
    }

    @Test
    @DisplayName("verify 실패 후에도 QueryCountContext가 초기화된다.")
    void verifyShouldClearContextEvenOnFailure() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");

        // when
        try {
            QueryCounterAssertion.assertCounts().select(2).verify();
        } catch (AssertionError ignored) {
        }

        // then
        assertThat(QueryCountContext.getQueries()).isEmpty();
    }

    @Test
    @DisplayName("forTables에 빈 리스트를 전달하면 전체 쿼리를 검증한다.")
    void forTablesWithEmptyListShouldVerifyAllQueries() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM product");

        // expected
        QueryCounterAssertion.assertCounts()
            .forTables(List.of())
            .select(2)
            .verify();
    }

    @Test
    @DisplayName("복합 조건에서 카운트 오류와 테이블별 오류가 모두 포함된다.")
    void verifyShouldIncludeBothGlobalAndTableErrors() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM product");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(3)
            .forTable("member").insert(1)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.SELECT: expected 3, but was 2")
            .hasMessageContaining("Table 'member' - QueryType.INSERT: expected 1, but was 0");
    }

}