package soon.springtestutil.querycount.assertion;

import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.context.QueryInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 쿼리 카운트 검증을 수행하는 내부 클래스
 */
class QueryCountVerifier {

    private static final int MAX_VIOLATIONS_TO_REPORT = 3;

    private final Map<QueryType, Long> expectedCounts;
    private final Map<String, TableQueryAssertion> tableAssertions;
    private final Set<String> tableNames;
    private final Long maxExecutionTimeMs;

    // 캐싱된 쿼리 목록
    private List<QueryInfo> cachedQueries;

    QueryCountVerifier(
        Map<QueryType, Long> expectedCounts,
        Map<String, TableQueryAssertion> tableAssertions,
        Set<String> tableNames,
        Long maxExecutionTimeMs
    ) {
        this.expectedCounts = expectedCounts;
        this.tableAssertions = tableAssertions;
        this.tableNames = tableNames;
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }

    /**
     * 기록된 쿼리가 하나도 없는데 검증이 실패했을 때 덧붙이는 안내입니다.
     *
     * <p>대개 {@code query-counter.enabled=true}를 설정하지 않아 DataSource가 감싸지지 않은
     * 경우입니다. 원인을 알기 어려운 실패라 힌트를 남깁니다.
     */
    static final String NO_QUERY_RECORDED_HINT =
        "No query was recorded. Is query-counter.enabled=true set in your test configuration?";

    void verify() {
        this.cachedQueries = QueryCountContext.getQueries();

        List<String> errors = collectErrors();

        if (!errors.isEmpty()) {
            if (this.cachedQueries.isEmpty()) {
                errors.add(NO_QUERY_RECORDED_HINT);
            }
            throw new AssertionError(TestContextHolder.getContextInfo() + String.join("\n\n", errors));
        }
    }

    private List<String> collectErrors() {
        List<String> errors = new ArrayList<>();
        verifyQueryCounts(errors);
        verifyTableQueryCounts(errors);
        collectExecutionTimeErrors(errors);
        return errors;
    }

    private void verifyQueryCounts(List<String> errors) {
        if (expectedCounts.isEmpty()) {
            return;
        }

        EnumMap<QueryType, Long> actualCounts = getActualCounts();
        String countErrors = expectedCounts.entrySet().stream()
            .map(entry -> compareCount(entry.getKey(), entry.getValue(), actualCounts))
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));

        if (!countErrors.isEmpty()) {
            errors.add("Query count assertion failed:\n" + countErrors);
        }
    }

    private String compareCount(QueryType type, long expected, Map<QueryType, Long> actual) {
        long actualCount = actual.getOrDefault(type, 0L);
        if (expected != actualCount) {
            return String.format("QueryType.%s: expected %d, but was %d", type, expected, actualCount);
        }
        return null;
    }

    private void verifyTableQueryCounts(List<String> errors) {
        if (tableAssertions.isEmpty()) {
            return;
        }

        List<String> tableErrors = new ArrayList<>();
        for (TableQueryAssertion tableAssertion : tableAssertions.values()) {
            tableErrors.addAll(verifyTableAssertion(tableAssertion));
        }

        if (!tableErrors.isEmpty()) {
            errors.add("Table-specific query count assertion failed:\n" + String.join("\n", tableErrors));
        }
    }

    private List<String> verifyTableAssertion(TableQueryAssertion assertion) {
        String tableName = assertion.getTableName();
        Map<QueryType, Long> expected = assertion.getExpectedCounts();

        EnumMap<QueryType, Long> actualCounts = cachedQueries.stream()
            .filter(q -> q.getTableNames().contains(tableName))
            .collect(Collectors.groupingBy(
                QueryInfo::getQueryType,
                () -> new EnumMap<>(QueryType.class),
                Collectors.counting()
            ));

        List<String> errors = new ArrayList<>();
        for (Map.Entry<QueryType, Long> entry : expected.entrySet()) {
            long actual = actualCounts.getOrDefault(entry.getKey(), 0L);
            if (!entry.getValue().equals(actual)) {
                errors.add(String.format("Table '%s' - QueryType.%s: expected %d, but was %d",
                    tableName, entry.getKey(), entry.getValue(), actual));
            }
        }
        return errors;
    }

    private void collectExecutionTimeErrors(List<String> errors) {
        collectGlobalExecutionTimeErrors(errors);
        collectTableExecutionTimeErrors(errors);
    }

    private void collectGlobalExecutionTimeErrors(List<String> errors) {
        if (maxExecutionTimeMs == null) {
            return;
        }

        List<QueryInfo> violations = getFilteredQueriesForTimeCheck().stream()
            .filter(q -> q.getExecutionTimeMs() != null && q.getExecutionTimeMs() > maxExecutionTimeMs)
            .toList();

        if (!violations.isEmpty()) {
            errors.add(formatExecutionTimeError(violations, maxExecutionTimeMs, null));
        }
    }

    private void collectTableExecutionTimeErrors(List<String> errors) {
        for (TableQueryAssertion tableAssertion : tableAssertions.values()) {
            Long tableMaxTime = tableAssertion.getMaxExecutionTimeMs();
            if (tableMaxTime == null) {
                continue;
            }

            String tableName = tableAssertion.getTableName();

            // expectedCounts가 비어있으면 해당 테이블의 모든 쿼리 타입을 검사
            Set<QueryType> types = tableAssertion.getExpectedCounts().isEmpty()
                ? null
                : tableAssertion.getExpectedCounts().keySet();

            List<QueryInfo> violations = cachedQueries.stream()
                .filter(q -> q.getTableNames().contains(tableName))
                .filter(q -> types == null || types.contains(q.getQueryType()))
                .filter(q -> q.getExecutionTimeMs() != null && q.getExecutionTimeMs() > tableMaxTime)
                .toList();

            if (!violations.isEmpty()) {
                errors.add(formatExecutionTimeError(violations, tableMaxTime, tableName));
            }
        }
    }

    private String formatExecutionTimeError(List<QueryInfo> violations, long maxTime, String tableName) {
        String prefix = tableName != null
            ? String.format("Table '%s' execution time", tableName)
            : "Query execution time";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s assertion failed: max=%dms, violations=%d",
            prefix, maxTime, violations.size()));

        int reportCount = Math.min(violations.size(), MAX_VIOLATIONS_TO_REPORT);
        for (int i = 0; i < reportCount; i++) {
            QueryInfo violation = violations.get(i);
            sb.append(String.format("\n[%d] %dms > %dms, type=%s, SQL: %s",
                i + 1,
                violation.getExecutionTimeMs(),
                maxTime,
                violation.getQueryType(),
                violation.getQuery()));
        }

        if (violations.size() > MAX_VIOLATIONS_TO_REPORT) {
            sb.append(String.format("\n... and %d more violations", violations.size() - MAX_VIOLATIONS_TO_REPORT));
        }

        return sb.toString();
    }

    private EnumMap<QueryType, Long> getActualCounts() {
        if (tableNames == null || tableNames.isEmpty()) {
            return cachedQueries.stream()
                .collect(Collectors.groupingBy(
                    QueryInfo::getQueryType,
                    () -> new EnumMap<>(QueryType.class),
                    Collectors.counting()
                ));
        }
        return cachedQueries.stream()
            .filter(this::hasTableOverlap)
            .collect(Collectors.groupingBy(
                QueryInfo::getQueryType,
                () -> new EnumMap<>(QueryType.class),
                Collectors.counting()
            ));
    }

    private List<QueryInfo> getFilteredQueriesForTimeCheck() {
        boolean hasTableFilter = tableNames != null && !tableNames.isEmpty();
        boolean hasTypeFilter = !expectedCounts.isEmpty();

        Set<QueryType> types = hasTypeFilter ? expectedCounts.keySet() : null;
        if (!hasTableFilter && !hasTypeFilter) {
            return cachedQueries;
        }

        return cachedQueries.stream()
            .filter(q -> !hasTableFilter || hasTableOverlap(q))
            .filter(q -> !hasTypeFilter || types.contains(q.getQueryType()))
            .collect(Collectors.toList());
    }

    /**
     * 쿼리가 필터링 대상 테이블과 공통 테이블을 가지는지 확인
     */
    private boolean hasTableOverlap(QueryInfo queryInfo) {
        return queryInfo.getTableNames().stream().anyMatch(tableNames::contains);
    }

}