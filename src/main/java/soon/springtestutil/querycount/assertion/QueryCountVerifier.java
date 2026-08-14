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

    private final Map<QueryType, ExpectedCount> expectedCounts;
    private final Map<String, TableQueryAssertion> tableAssertions;
    private final Set<String> tableNames;
    private final Long maxExecutionTimeMs;
    private final boolean noNPlusOne;

    // 캐싱된 쿼리 목록
    private List<QueryInfo> cachedQueries;

    QueryCountVerifier(
        Map<QueryType, ExpectedCount> expectedCounts,
        Map<String, TableQueryAssertion> tableAssertions,
        Set<String> tableNames,
        Long maxExecutionTimeMs,
        boolean noNPlusOne
    ) {
        this.expectedCounts = expectedCounts;
        this.tableAssertions = tableAssertions;
        this.tableNames = tableNames;
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        this.noNPlusOne = noNPlusOne;
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
        collectNPlusOneErrors(errors);
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

    private String compareCount(QueryType type, ExpectedCount expected, Map<QueryType, Long> actual) {
        long actualCount = actual.getOrDefault(type, 0L);
        if (!expected.matches(actualCount)) {
            return String.format("QueryType.%s: %s, but was %d", type, expected.describe(), actualCount)
                + describeQueries(queriesOfType(type, null));
        }
        return null;
    }

    private List<QueryInfo> queriesOfType(QueryType type, String tableName) {
        return cachedQueries.stream()
            .filter(q -> q.getQueryType() == type)
            .filter(q -> tableName != null ? q.getTableNames().contains(tableName) : matchesTableFilter(q))
            .toList();
    }

    private boolean matchesTableFilter(QueryInfo queryInfo) {
        return tableNames == null || tableNames.isEmpty() || hasTableOverlap(queryInfo);
    }

    /**
     * 실제로 나간 SQL 을 실패 메시지에 붙입니다. 숫자만 알려주면 어느 쿼리가 늘었는지 찾으려고
     * SQL 로깅을 켜고 테스트를 다시 돌려야 합니다.
     */
    private String describeQueries(List<QueryInfo> queries) {
        if (queries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int reportCount = Math.min(queries.size(), MAX_VIOLATIONS_TO_REPORT);
        for (int i = 0; i < reportCount; i++) {
            sb.append(String.format("%n  [%d] %s", i + 1, queries.get(i).getQuery()));
        }
        if (queries.size() > MAX_VIOLATIONS_TO_REPORT) {
            sb.append(String.format("%n  ... and %d more", queries.size() - MAX_VIOLATIONS_TO_REPORT));
        }
        return sb.toString();
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
        Map<QueryType, ExpectedCount> expected = assertion.getExpectedCounts();

        EnumMap<QueryType, Long> actualCounts = cachedQueries.stream()
            .filter(q -> q.getTableNames().contains(tableName))
            .collect(Collectors.groupingBy(
                QueryInfo::getQueryType,
                () -> new EnumMap<>(QueryType.class),
                Collectors.counting()
            ));

        List<String> errors = new ArrayList<>();
        for (Map.Entry<QueryType, ExpectedCount> entry : expected.entrySet()) {
            long actual = actualCounts.getOrDefault(entry.getKey(), 0L);
            if (!entry.getValue().matches(actual)) {
                errors.add(String.format("Table '%s' - QueryType.%s: %s, but was %d",
                    tableName, entry.getKey(), entry.getValue().describe(), actual)
                    + describeQueries(queriesOfType(entry.getKey(), tableName)));
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

    private void collectNPlusOneErrors(List<String> errors) {
        if (!noNPlusOne) {
            return;
        }

        List<QueryInfo> target = (tableNames == null || tableNames.isEmpty())
            ? cachedQueries
            : cachedQueries.stream().filter(this::hasTableOverlap).toList();

        List<NPlusOneDetector.Finding> findings = NPlusOneDetector.detect(target);
        if (!findings.isEmpty()) {
            errors.add(formatNPlusOneError(findings));
        }
    }

    private String formatNPlusOneError(List<NPlusOneDetector.Finding> findings) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("N+1 assertion failed: %d query %s ran with different parameter values",
            findings.size(), findings.size() == 1 ? "shape" : "shapes"));

        int reportCount = Math.min(findings.size(), MAX_VIOLATIONS_TO_REPORT);
        for (int i = 0; i < reportCount; i++) {
            NPlusOneDetector.Finding finding = findings.get(i);
            sb.append(String.format("\n[%d] %d executions, %d distinct parameter values\n    SQL: %s\n    params: %s",
                i + 1,
                finding.executionCount(),
                finding.distinctParameters().size(),
                finding.sql(),
                describeParameters(finding.distinctParameters())));
        }

        if (findings.size() > MAX_VIOLATIONS_TO_REPORT) {
            sb.append(String.format("\n... and %d more", findings.size() - MAX_VIOLATIONS_TO_REPORT));
        }

        return sb.toString();
    }

    private String describeParameters(List<List<List<Object>>> distinctParameters) {
        return distinctParameters.stream()
            .limit(MAX_VIOLATIONS_TO_REPORT)
            .map(sets -> sets.size() == 1 ? sets.get(0).toString() : sets.toString())
            .collect(Collectors.joining(", "))
            + (distinctParameters.size() > MAX_VIOLATIONS_TO_REPORT ? ", ..." : "");
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
