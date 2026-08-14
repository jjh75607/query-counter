package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(QueryCountTestExtension.class)
class FailureMessageSqlTest {

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    @DisplayName("카운트가 어긋나면 실제로 나간 SQL 을 함께 보여준다")
    @Test
    void showsActualQueriesWhenCountDoesNotMatch() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select * from member");
        QueryCountContext.addQuery(QueryType.SELECT, "select * from team where id=?");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts().select(1).verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected 1, but was 2")
            .hasMessageContaining("[1] select * from member")
            .hasMessageContaining("[2] select * from team where id=?");
    }

    @DisplayName("SQL 이 세 개를 넘으면 나머지 개수만 알려준다")
    @Test
    void truncatesWhenThereAreManyQueries() {
        // given
        for (int i = 1; i <= 5; i++) {
            QueryCountContext.addQuery(QueryType.SELECT, "select * from member where id=" + i);
        }

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts().select(1).verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[3] select * from member where id=3")
            .hasMessageContaining("... and 2 more")
            .hasMessageNotContaining("[4]");
    }

    @DisplayName("forTables 로 좁히면 그 테이블의 SQL 만 보여준다")
    @Test
    void showsOnlyQueriesOfTheFilteredTables() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select * from member");
        QueryCountContext.addQuery(QueryType.SELECT, "select * from team");

        // expected
        assertThatThrownBy(() ->
            QueryCounterAssertion.assertCounts().forTables("member").select(5).verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("select * from member")
            .hasMessageNotContaining("select * from team");
    }

    @DisplayName("테이블별 검증이 어긋나면 그 테이블의 SQL 을 보여준다")
    @Test
    void showsQueriesForTableSpecificFailure() {
        // given
        QueryCountContext.addQuery(QueryType.INSERT, "insert into member (name) values (?)");
        QueryCountContext.addQuery(QueryType.SELECT, "select * from team");

        // expected
        assertThatThrownBy(() ->
            QueryCounterAssertion.assertCounts().forTable("member").insert(2).verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Table 'member'")
            .hasMessageContaining("insert into member (name) values (?)")
            .hasMessageNotContaining("select * from team");
    }

    @DisplayName("기대한 타입의 쿼리가 하나도 없으면 SQL 목록을 붙이지 않는다")
    @Test
    void addsNothingWhenThereAreNoQueriesOfThatType() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select * from member");

        // expected - INSERT 를 기대했으나 하나도 없다
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts().insert(1).verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.INSERT: expected 1, but was 0")
            .hasMessageNotContaining("[1]");
    }

}
