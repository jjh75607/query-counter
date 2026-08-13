package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(QueryCountTestExtension.class)
class NPlusOneAssertionTest {

    private static final String SELECT_TEAM = "select t.id, t.name from team t where t.id=?";

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    private void addSelectWithParam(String sql, Object value) {
        QueryCountContext.addQuery(QueryType.SELECT, sql, null, List.of(List.of(value)));
    }

    @DisplayName("같은 SELECT 가 서로 다른 파라미터 값으로 반복되면 실패한다")
    @Test
    void failsWhenSameSelectRunsWithDifferentParameters() {
        // given
        addSelectWithParam(SELECT_TEAM, 1);
        addSelectWithParam(SELECT_TEAM, 2);
        addSelectWithParam(SELECT_TEAM, 3);

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts().noNPlusOne().verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("N+1 assertion failed")
            .hasMessageContaining("3 executions")
            .hasMessageContaining(SELECT_TEAM);
    }

    @DisplayName("같은 SELECT 라도 파라미터 값이 같으면 N+1 이 아니다")
    @Test
    void passesWhenSameSelectRunsWithSameParameters() {
        // given - 중복 조회이지 부모 한 건마다 자식을 읽은 모양이 아니다
        addSelectWithParam(SELECT_TEAM, 1);
        addSelectWithParam(SELECT_TEAM, 1);
        addSelectWithParam(SELECT_TEAM, 1);

        // expected
        assertThatCode(() -> QueryCounterAssertion.assertCounts().noNPlusOne().verify())
            .doesNotThrowAnyException();
    }

    @DisplayName("SELECT 가 한 번만 실행되면 N+1 이 아니다")
    @Test
    void passesWhenSelectRunsOnce() {
        // given
        addSelectWithParam(SELECT_TEAM, 1);

        // expected
        assertThatCode(() -> QueryCounterAssertion.assertCounts().noNPlusOne().verify())
            .doesNotThrowAnyException();
    }

    @DisplayName("SELECT 가 아닌 쿼리는 파라미터가 달라도 N+1 로 보지 않는다")
    @Test
    void ignoresNonSelectQueries() {
        // given
        String insert = "insert into member (id, name) values (?, ?)";
        QueryCountContext.addQuery(QueryType.INSERT, insert, null, List.of(List.of(1, "a")));
        QueryCountContext.addQuery(QueryType.INSERT, insert, null, List.of(List.of(2, "b")));

        // expected
        assertThatCode(() -> QueryCounterAssertion.assertCounts().noNPlusOne().verify())
            .doesNotThrowAnyException();
    }

    @DisplayName("배치로 나간 실행은 파라미터 세트가 여럿이어도 왕복 한 번이라 N+1 이 아니다")
    @Test
    void treatsOneBatchExecutionAsSingleRoundTrip() {
        // given - 세트가 셋이지만 실행은 하나다
        QueryCountContext.addQuery(QueryType.SELECT, SELECT_TEAM, null,
            List.of(List.of(1), List.of(2), List.of(3)));

        // expected
        assertThatCode(() -> QueryCounterAssertion.assertCounts().noNPlusOne().verify())
            .doesNotThrowAnyException();
    }

    @DisplayName("forTables 로 좁히면 그 테이블의 쿼리만 N+1 로 판정한다")
    @Test
    void honoursTableFilter() {
        // given - team 은 N+1 이지만 member 로 좁힌다
        addSelectWithParam(SELECT_TEAM, 1);
        addSelectWithParam(SELECT_TEAM, 2);

        // expected
        assertThatCode(() -> QueryCounterAssertion.assertCounts().forTables("member").noNPlusOne().verify())
            .doesNotThrowAnyException();
    }

    @DisplayName("noNPlusOne 을 부르지 않으면 N+1 이 있어도 통과한다")
    @Test
    void doesNothingWhenNotRequested() {
        // given
        addSelectWithParam(SELECT_TEAM, 1);
        addSelectWithParam(SELECT_TEAM, 2);

        // expected
        assertThatCode(() -> QueryCounterAssertion.assertCounts().select(2).verify())
            .doesNotThrowAnyException();
    }

}
