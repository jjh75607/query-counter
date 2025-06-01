package soon.springtestutil.querycount.assertion;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.QueryType;

public class QueryCounterAssertion {

    private final Map<QueryType, Long> expectedCounts;

    private QueryCounterAssertion() {
        this.expectedCounts = new EnumMap<>(QueryType.class);
        for (QueryType type : QueryType.values()) {
            this.expectedCounts.put(type, 0L);
        }
    }

    public static QueryCounterAssertion assertCounts() {
        return new QueryCounterAssertion();
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
        EnumMap<QueryType, Long> actualCounts = QueryCountContext.getQueryCounts();

        String errors = Arrays.stream(QueryType.values())
            .map(type -> {
                long expected = expectedCounts.getOrDefault(type, 0L);
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

}