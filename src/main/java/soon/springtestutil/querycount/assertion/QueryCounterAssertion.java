package soon.springtestutil.querycount.assertion;

import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.context.QueryInfo;

import java.util.*;
import java.util.stream.Collectors;

public class QueryCounterAssertion {

    private final Map<QueryType, Long> expectedCounts = new EnumMap<>(QueryType.class);
    private Set<String> tableNames;
    private Long maxExecutionTimeMs;

    private QueryCounterAssertion() {
    }

    public static QueryCounterAssertion assertCounts() {
        return new QueryCounterAssertion();
    }

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

    // TODO: 현재는 각 쿼리별 측정, 통합 실행 시간 측정 기능 추가
    public QueryCounterAssertion maxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        return this;
    }

    public void verify() {
        EnumMap<QueryType, Long> actualCounts = getActualCounts();

        String errors = expectedCounts.keySet().stream()
            .map(type -> {
                long expected = expectedCounts.get(type);
                long actual = actualCounts.getOrDefault(type, 0L);
                if (expected != actual) {
                    return String.format("QueryType.%s: expected %d, but was %d", type, expected,
                        actual);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));

        if (!errors.isEmpty()) {
            QueryCountContext.clear();

            String contextInfo = TestContextHolder.getContextInfo();
            throw new AssertionError(contextInfo + "Query count assertion failed:\n" + errors);
        }

        if (maxExecutionTimeMs != null) {
            List<QueryInfo> filtered = getFilteredQueriesForTimeCheck();
            List<QueryInfo> violations = filtered.stream()
                .filter(q -> q.getExecutionTimeMs() != null && q.getExecutionTimeMs() > maxExecutionTimeMs)
                .toList();

            if (!violations.isEmpty()) {
                QueryInfo first = violations.get(0);
                String contextInfo = TestContextHolder.getContextInfo();
                QueryCountContext.clear();
                String message = String.format(
                    "%sQuery execution time assertion failed: max=%dms, violations=%d\nFirst violation: %dms > %dms, type=%s\nSQL: %s",
                    contextInfo,
                    maxExecutionTimeMs,
                    violations.size(),
                    first.getExecutionTimeMs(),
                    maxExecutionTimeMs,
                    first.getQueryType(),
                    first.getQuery()
                );
                throw new AssertionError(message);
            }
        }

        QueryCountContext.clear();
    }

    private EnumMap<QueryType, Long> getActualCounts() {
        if (tableNames == null || tableNames.isEmpty()) {
            return QueryCountContext.getQueryCounts();
        }
        return QueryCountContext.getQueries().stream()
            .filter(queryInfo -> !Collections.disjoint(queryInfo.getTableNames(), tableNames))
            .collect(Collectors.groupingBy(
                QueryInfo::getQueryType,
                () -> new EnumMap<>(QueryType.class),
                Collectors.counting()
            ));
    }

    private List<QueryInfo> getFilteredQueriesForTimeCheck() {
        List<QueryInfo> base = QueryCountContext.getQueries();
        if (tableNames != null && !tableNames.isEmpty()) {
            base = base.stream()
                .filter(q -> !Collections.disjoint(q.getTableNames(), tableNames))
                .collect(Collectors.toList());
        }

        if (!expectedCounts.isEmpty()) {
            Set<QueryType> types = expectedCounts.keySet();
            base = base.stream()
                .filter(q -> types.contains(q.getQueryType()))
                .collect(Collectors.toList());
        }

        return base;
    }

}