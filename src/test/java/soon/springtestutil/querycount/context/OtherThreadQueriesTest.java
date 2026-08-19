package soon.springtestutil.querycount.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.querycount.QueryType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("다른 스레드 쿼리 수집소")
class OtherThreadQueriesTest {

    @BeforeEach
    @AfterEach
    void clearAll() {
        OtherThreadQueries.clear();
        QueryCountContext.clear();
    }

    private QueryInfo select(String sql) {
        return new QueryInfo(QueryType.SELECT, sql, 1L, List.of());
    }

    @DisplayName("담은 것을 가져가면 수집소가 비워진다")
    @Test
    void drainShouldEmptyTheCollector() {
        // given
        OtherThreadQueries.add(select("select 1"));
        OtherThreadQueries.add(select("select 2"));

        // when
        List<QueryInfo> drained = OtherThreadQueries.drain();

        // then
        assertThat(drained).hasSize(2);
        assertThat(OtherThreadQueries.size()).isZero();
    }

    @DisplayName("가져가지 않고 비우면 남지 않는다")
    @Test
    void clearShouldDiscardWithoutDraining() {
        // given
        OtherThreadQueries.add(select("select 1"));

        // when
        OtherThreadQueries.clear();

        // then
        assertThat(OtherThreadQueries.size()).isZero();
        assertThat(OtherThreadQueries.drain()).isEmpty();
    }

    @DisplayName("합치면 이 스레드의 기록에 들어가고 수집소는 비워진다")
    @Test
    void mergeShouldMoveIntoThreadRecord() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select on test thread", 1L, List.of());
        OtherThreadQueries.add(select("select on other thread"));

        // when
        QueryCountContext.mergeOtherThreadQueries();

        // then
        assertThat(QueryCountContext.getQueries())
            .extracting(QueryInfo::getQuery)
            .containsExactly("select on test thread", "select on other thread");
        assertThat(OtherThreadQueries.size()).isZero();
    }

    @DisplayName("테스트 기록을 비우면 수집소도 함께 비워진다")
    @Test
    void clearContextShouldAlsoClearTheCollector() {
        // given - 앞 테스트가 남긴 것이 다음 테스트로 새지 않는지가 이 규율이다
        OtherThreadQueries.add(select("select 1"));

        // when
        QueryCountContext.clear();

        // then
        assertThat(OtherThreadQueries.size()).isZero();
    }

    @DisplayName("합칠 것이 없으면 기록이 그대로다")
    @Test
    void mergeShouldDoNothingWhenEmpty() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1", 1L, List.of());

        // when
        QueryCountContext.mergeOtherThreadQueries();

        // then
        assertThat(QueryCountContext.getQueries()).hasSize(1);
    }

}
