package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableQueryAssertionTest {

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    @Test
    @DisplayName("TableQueryAssertion은 체이닝을 통해 여러 쿼리 타입을 설정할 수 있다")
    void shouldSupportMethodChaining() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id) VALUES (1)");
        QueryCountContext.addQuery(QueryType.UPDATE, "UPDATE member SET name='test'");
        QueryCountContext.addQuery(QueryType.DELETE, "DELETE FROM member WHERE id=1");

        // expected
        QueryCounterAssertion.assertCounts().forTable("member")
            .select(1)
            .insert(1)
            .update(1)
            .delete(1)
            .verify();
    }

    @Test
    @DisplayName("동일 테이블을 여러 번 참조해도 마지막 설정이 적용된다")
    void shouldUseLastSettingForSameTable() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");

        // expected
        QueryCounterAssertion.assertCounts()
            .forTable("member").select(1)
            .forTable("member").select(2)
            .verify();
    }

    @Test
    @DisplayName("others 쿼리 타입을 검증할 수 있다")
    void shouldVerifyOthersQueryType() {
        // given
        QueryCountContext.addQuery(QueryType.OTHERS, "TRUNCATE TABLE test");

        // expected
        QueryCounterAssertion.assertCounts()
            .others(1)
            .verify();
    }

    @Test
    @DisplayName("테이블별 maxExecutionTimeMs 설정이 독립적으로 적용된다")
    void shouldApplyMaxExecutionTimeIndependentlyPerTable() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member", 80L);
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM product", 150L);

        // expected
        QueryCounterAssertion.assertCounts()
            .forTable("member").select(1).maxExecutionTimeMs(100)
            .forTable("product").select(1).maxExecutionTimeMs(200)
            .verify();
    }

    @Test
    @DisplayName("테이블별 maxExecutionTimeMs 초과 시 해당 테이블 정보가 에러에 포함된다")
    void shouldIncludeTableNameInExecutionTimeError() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member", 150L);

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .forTable("member").select(1).maxExecutionTimeMs(100)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Table 'member'")
            .hasMessageContaining("execution time assertion failed");
    }

    @Test
    @DisplayName("forTable에서 forTables로 전환할 수 있다")
    void shouldSwitchFromForTableToForTables() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM orders");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO product (id) VALUES (1)");

        // expected
        QueryCounterAssertion.assertCounts()
            .forTable("product").insert(1)
            .forTables("member", "orders").select(2)
            .verify();
    }

    @Test
    @DisplayName("쿼리 카운트 없이 maxExecutionTimeMs만 설정해도 검증된다")
    void shouldVerifyOnlyExecutionTimeWithoutCount() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member", 50L);

        // expected
        QueryCounterAssertion.assertCounts()
            .forTable("member").maxExecutionTimeMs(100)
            .verify();
    }

}
