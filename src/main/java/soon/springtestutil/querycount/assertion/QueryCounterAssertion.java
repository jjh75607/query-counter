package soon.springtestutil.querycount.assertion;

import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.*;

/**
 * Builder for asserting the number of queries executed during a test.
 *
 * <p>Expected counts are plain {@code long} values, so they can be computed at runtime.
 * This makes it possible to express expectations that depend on test data.
 *
 * <pre>{@code
 * QueryCounterAssertion.assertCounts()
 *     .select(1)
 *     .insert(members.size())
 *     .verify();
 * }</pre>
 *
 * <p>Requires {@code query-counter.enabled=true} in the test configuration.
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
     * Sets assertions for a single table. Chain this call to assert different
     * expectations for several tables.
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
     * Restricts the assertion to the given tables. Queries touching any of them are
     * counted together. When not specified, queries against every table are counted.
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