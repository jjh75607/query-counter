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

    void verify() {
        this.cachedQueries = QueryCountContext.getQueries();

        List<String> errors = collectErrors();

        if (!errors.isEmpty()) {
            throw new AssertionError(TestContextHolder.getContextInfo() + String.join("\n\n", errors));
        }

        verifyExecutionTimes();
    }

    private List<String> collectErrors() {
        List<String> errors = new ArrayList<>();
        verifyQueryCounts(errors);
        verifyTableQueryCounts(errors);
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

        return expected.entrySet().stream()
            .filter(entry -> !entry.getValue().equals(actualCounts.getOrDefault(entry.getKey(), 0L)))
            .map(entry -> String.format("Table '%s' - QueryType.%s: expected %d, but was %d",
                tableName, entry.getKey(), entry.getValue(), actualCounts.getOrDefault(entry.getKey(), 0L)))
            .collect(Collectors.toList());
    }

    private void verifyExecutionTimes() {
        verifyGlobalExecutionTime();
        verifyTableExecutionTime();
    }

    private void verifyGlobalExecutionTime() {
        if (maxExecutionTimeMs == null) {
            return;
        }

        List<QueryInfo> violations = getFilteredQueriesForTimeCheck().stream()
            .filter(q -> q.getExecutionTimeMs() != null && q.getExecutionTimeMs() > maxExecutionTimeMs)
            .toList();

        if (!violations.isEmpty()) {
            throw createExecutionTimeError(violations.get(0), maxExecutionTimeMs, violations.size(), null);
        }
    }

    private void verifyTableExecutionTime() {

        for (TableQueryAssertion tableAssertion : tableAssertions.values()) {
            Long tableMaxTime = tableAssertion.getMaxExecutionTimeMs();
            if (tableMaxTime == null) {
                continue;
            }

            String tableName = tableAssertion.getTableName();
            Set<QueryType> types = tableAssertion.getExpectedCounts().isEmpty()
                ? null
                : tableAssertion.getExpectedCounts().keySet();

            List<QueryInfo> violations = cachedQueries.stream()
                .filter(q -> q.getTableNames().contains(tableName))
                .filter(q -> types == null || types.contains(q.getQueryType()))
                .filter(q -> q.getExecutionTimeMs() != null && q.getExecutionTimeMs() > tableMaxTime)
                .toList();

            if (!violations.isEmpty()) {
                throw createExecutionTimeError(violations.get(0), tableMaxTime, violations.size(), tableName);
            }
        }
    }

    private AssertionError createExecutionTimeError(QueryInfo violation, long maxTime, int count, String tableName) {
        String prefix = tableName != null
            ? String.format("Table '%s' execution time", tableName)
            : "Query execution time";

        String message = String.format(
            "%s%s assertion failed: max=%dms, violations=%d\nFirst violation: %dms > %dms, type=%s\nSQL: %s",
            TestContextHolder.getContextInfo(),
            prefix,
            maxTime,
            count,
            violation.getExecutionTimeMs(),
            maxTime,
            violation.getQueryType(),
            violation.getQuery()
        );
        return new AssertionError(message);
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
            .filter(queryInfo -> !Collections.disjoint(queryInfo.getTableNames(), tableNames))
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
            .filter(q -> !hasTableFilter || !Collections.disjoint(q.getTableNames(), tableNames))
            .filter(q -> !hasTypeFilter || types.contains(q.getQueryType()))
            .collect(Collectors.toList());
    }

}