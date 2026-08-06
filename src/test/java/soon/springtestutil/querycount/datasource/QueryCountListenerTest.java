package soon.springtestutil.querycount.datasource;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class QueryCountListenerTest {

    private QueryCountListener listener;
    private ExecutionInfo execInfo;

    @BeforeEach
    void setUp() {
        listener = new QueryCountListener();
        execInfo = mock(ExecutionInfo.class);
    }

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    @DisplayName("SELECT 쿼리가 실행되면 SELECT의 카운트가 증가한다.")
    @Test
    void afterQueryWithSelectQueryIncrementsSelectCount() {
        // given
        QueryInfo queryInfo = createMockQueryInfo("SELECT * FROM users");
        List<QueryInfo> queryInfos = Collections.singletonList(queryInfo);

        // when
        listener.afterQuery(execInfo, queryInfos);

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(QueryType.SELECT, 1L);
    }

    @DisplayName("여러 다른 유형의 쿼리가 실행되면 각 유형의 카운트가 증가한다")
    @Test
    void afterQueryWithMultipleDifferentQueriesIncrementsCorrectCounts() {
        // given
        QueryInfo selectQuery = createMockQueryInfo("SELECT * FROM users");
        QueryInfo insertQuery = createMockQueryInfo("INSERT INTO products VALUES (1, 'test')");
        List<QueryInfo> queryInfoList = List.of(selectQuery, insertQuery);

        // when
        listener.afterQuery(execInfo, queryInfoList);

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(QueryType.SELECT, 1L);
        assertThat(counts).containsEntry(QueryType.INSERT, 1L);
    }

    @DisplayName("여러 동일한 유형의 쿼리 후 해당 유형의 카운트가 증가한다")
    @Test
    void afterQueryWithMultipleIdenticalQueriesIncrementsCountCorrectly() {
        // given
        QueryInfo selectQuery1 = createMockQueryInfo("SELECT name FROM customers");
        QueryInfo selectQuery2 = createMockQueryInfo("SELECT name FROM customers");
        List<QueryInfo> queryInfoList = List.of(selectQuery1, selectQuery2);

        // when
        listener.afterQuery(execInfo, queryInfoList);

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(QueryType.SELECT, 2L);
    }

    @DisplayName("유효하지 않은 SQL이 들어오면 OTHERS로 카운트된다")
    @Test
    void invalidSqlShouldBeCountedAsOthers() {
        // given
        QueryInfo invalidQuery = createMockQueryInfo("INVALID SQL");
        List<QueryInfo> queryInfoList = List.of(invalidQuery);

        // when
        listener.afterQuery(execInfo, queryInfoList);

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(QueryType.OTHERS, 1L);
    }

    @DisplayName("동일한 SQL도 실행시간은 각각 저장된다")
    @Test
    void elapsedTimeCapturedIndependentlyForCachedQueryType() {
        // given
        given(execInfo.getElapsedTime()).willReturn(10L, 22L);
        QueryInfo q1 = createMockQueryInfo("SELECT * FROM cache_test");
        QueryInfo q2 = createMockQueryInfo("SELECT * FROM cache_test");

        // when
        listener.afterQuery(execInfo, List.of(q1));
        listener.afterQuery(execInfo, List.of(q2));

        // then
        var stored = QueryCountContext.getQueries();
        assertThat(stored).hasSize(2)
            .extracting("executionTimeMs")
            .containsExactly(10L, 22L);
    }

    private QueryInfo createMockQueryInfo(String sql) {
        QueryInfo mockQueryInfo = mock(QueryInfo.class);
        given(mockQueryInfo.getQuery())
            .willReturn(sql);
        return mockQueryInfo;
    }

}
