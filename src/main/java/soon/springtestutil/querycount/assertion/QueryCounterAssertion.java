package soon.springtestutil.querycount.assertion;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.context.QueryInfo;

public class QueryCounterAssertion {

    private final Map<QueryType, Long> expectedCounts;
    private Set<String> tableNames;

    private QueryCounterAssertion() {
        this.expectedCounts = new EnumMap<>(QueryType.class);
    }

    public static QueryCounterAssertion assertCounts() {
        return new QueryCounterAssertion();
    }

    public QueryCounterAssertion forTables(String... tableNames) {
        this.tableNames = new HashSet<>(Arrays.asList(tableNames)); // new HashSet
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

}