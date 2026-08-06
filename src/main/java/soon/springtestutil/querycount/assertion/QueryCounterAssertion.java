package soon.springtestutil.querycount.assertion;

import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.*;

/**
 * 쿼리 카운트 검증을 위한 빌더 클래스
 */
public class QueryCounterAssertion {

    private final Map<QueryType, Long> expectedCounts = new EnumMap<>(QueryType.class);
    private final Map<String, TableQueryAssertion> tableAssertions = new LinkedHashMap<>();
    private Set<String> tableNames;
    private Long maxExecutionTimeMs;

    private QueryCounterAssertion() {
    }

    public static QueryCounterAssertion assertCounts() {
        return new QueryCounterAssertion();
    }

    /**
     * 특정 테이블에 대한 쿼리 카운트 검증 조건을 설정합니다.
     * 체이닝을 통해 여러 테이블에 대해 각각 다른 검증 조건을 설정할 수 있습니다.
     *
     * <pre>{@code
     * QueryCounterAssertion.assertCounts()
     *     .forTable("member").insert(2).select(1)
     *     .forTable("product").select(3)
     *     .verify();
     * }</pre>
     */
    public TableQueryAssertion forTable(String tableName) {
        return tableAssertions.computeIfAbsent(tableName,
            name -> new TableQueryAssertion(this, name));
    }

    /**
     * 여러 테이블에 대한 통합 쿼리 카운트 검증 조건을 설정합니다.
     * 지정된 모든 테이블에서 발생한 쿼리의 총 합계를 검증합니다.
     */
    public QueryCounterAssertion forTables(String... tableNames) {
        this.tableNames = new HashSet<>(Arrays.asList(tableNames));
        return this;
    }

    public QueryCounterAssertion forTables(List<String> tableNames) {
        this.tableNames = new HashSet<>(tableNames);
        return this;
    }

    public QueryCounterAssertion select(long count) {
        expectedCounts.put(QueryType.SELECT, count);
        return this;
    }

    public QueryCounterAssertion insert(long count) {
        expectedCounts.put(QueryType.INSERT, count);
        return this;
    }

    public QueryCounterAssertion update(long count) {
        expectedCounts.put(QueryType.UPDATE, count);
        return this;
    }

    public QueryCounterAssertion delete(long count) {
        expectedCounts.put(QueryType.DELETE, count);
        return this;
    }

    public QueryCounterAssertion others(long count) {
        expectedCounts.put(QueryType.OTHERS, count);
        return this;
    }

    public QueryCounterAssertion maxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        return this;
    }

    public void verify() {
        if (!QueryCountContext.isActive()) {
            throw new IllegalStateException(
                "query-counter is disabled. "
                    + "Set query-counter.enabled=true in your test configuration."
            );
        }

        try {
            QueryCountVerifier verifier = new QueryCountVerifier(
                expectedCounts,
                tableAssertions,
                tableNames,
                maxExecutionTimeMs
            );
            verifier.verify();
        } finally {
            QueryCountContext.clear();
        }
    }

}