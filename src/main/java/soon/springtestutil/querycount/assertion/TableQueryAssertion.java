package soon.springtestutil.querycount.assertion;

import org.springframework.util.StringUtils;
import soon.springtestutil.querycount.QueryType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 특정 테이블에 대한 쿼리 카운트 검증 조건을 저장하는 클래스입니다.
 */
public class TableQueryAssertion {

    private final QueryCounterAssertion parent;
    private final String tableName;
    private final Map<QueryType, Long> expectedCounts = new EnumMap<>(QueryType.class);
    private Long maxExecutionTimeMs;

    TableQueryAssertion(QueryCounterAssertion parent, String tableName) {
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new IllegalArgumentException("tableName must not be null or blank");
        }
        this.parent = parent;
        this.tableName = tableName;
    }

    public TableQueryAssertion select(long count) {
        expectedCounts.put(QueryType.SELECT, count);
        return this;
    }

    public TableQueryAssertion insert(long count) {
        expectedCounts.put(QueryType.INSERT, count);
        return this;
    }

    public TableQueryAssertion update(long count) {
        expectedCounts.put(QueryType.UPDATE, count);
        return this;
    }

    public TableQueryAssertion delete(long count) {
        expectedCounts.put(QueryType.DELETE, count);
        return this;
    }

    public TableQueryAssertion others(long count) {
        expectedCounts.put(QueryType.OTHERS, count);
        return this;
    }

    public TableQueryAssertion maxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        return this;
    }

    /**
     * 다른 테이블에 대한 검증 조건을 추가합니다.
     */
    public TableQueryAssertion forTable(String tableName) {
        return parent.forTable(tableName);
    }

    /**
     * 여러 테이블에 대한 통합 검증 조건을 추가합니다.
     */
    public QueryCounterAssertion forTables(String... tableNames) {
        return parent.forTables(tableNames);
    }

    /**
     * 모든 검증을 수행합니다.
     */
    public void verify() {
        parent.verify();
    }

    String getTableName() {
        return tableName;
    }

    Map<QueryType, Long> getExpectedCounts() {
        return expectedCounts;
    }

    Long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

}