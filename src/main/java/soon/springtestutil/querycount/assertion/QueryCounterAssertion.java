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
 * <p>Calling {@link #verify()} is optional. An assertion that is created but never
 * verified is checked automatically once the test method finishes, so a forgotten
 * {@code verify()} can never make a test pass silently.
 *
 * <p>Requires {@code query-counter.enabled=true} in the test configuration.
 */
public class QueryCounterAssertion {

    /**
     * Assertions created on this thread that have not been verified yet.
     */
    private static final ThreadLocal<List<QueryCounterAssertion>> pending =
        ThreadLocal.withInitial(ArrayList::new);

    private final Map<QueryType, ExpectedCount> expectedCounts = new EnumMap<>(QueryType.class);
    private final Map<String, TableQueryAssertion> tableAssertions = new LinkedHashMap<>();
    private Set<String> tableNames;
    private Long maxExecutionTimeMs;

    private QueryCounterAssertion() {
    }

    public static QueryCounterAssertion assertCounts() {
        QueryCounterAssertion assertion = new QueryCounterAssertion();
        pending.get().add(assertion);
        return assertion;
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
        return select(ExpectedCount.exactly(count));
    }

    public QueryCounterAssertion select(ExpectedCount expected) {
        expectedCounts.put(QueryType.SELECT, expected);
        return this;
    }

    public QueryCounterAssertion insert(long count) {
        return insert(ExpectedCount.exactly(count));
    }

    public QueryCounterAssertion insert(ExpectedCount expected) {
        expectedCounts.put(QueryType.INSERT, expected);
        return this;
    }

    public QueryCounterAssertion update(long count) {
        return update(ExpectedCount.exactly(count));
    }

    public QueryCounterAssertion update(ExpectedCount expected) {
        expectedCounts.put(QueryType.UPDATE, expected);
        return this;
    }

    public QueryCounterAssertion delete(long count) {
        return delete(ExpectedCount.exactly(count));
    }

    public QueryCounterAssertion delete(ExpectedCount expected) {
        expectedCounts.put(QueryType.DELETE, expected);
        return this;
    }

    public QueryCounterAssertion others(long count) {
        return others(ExpectedCount.exactly(count));
    }

    public QueryCounterAssertion others(ExpectedCount expected) {
        expectedCounts.put(QueryType.OTHERS, expected);
        return this;
    }

    public QueryCounterAssertion maxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        return this;
    }

    public void verify() {
        try {
            doVerify();
        } finally {
            QueryCountContext.clear();
        }
    }

    /**
     * Verifies every assertion that was created but never verified, then clears the
     * recorded queries. Called automatically after each test method.
     *
     * <p>All remaining assertions are checked before the recorded queries are cleared,
     * so having several unverified assertions still reports each of them correctly.
     */
    public static void verifyPending() {
        List<QueryCounterAssertion> remaining = new ArrayList<>(pending.get());
        pending.remove();

        if (remaining.isEmpty()) {
            return;
        }

        try {
            for (QueryCounterAssertion assertion : remaining) {
                assertion.doVerify();
            }
        } finally {
            QueryCountContext.clear();
        }
    }

    /**
     * Discards assertions left over from a previous test on this thread.
     */
    public static void clearPending() {
        pending.remove();
    }

    private void doVerify() {
        pending.get().remove(this);

        QueryCountVerifier verifier = new QueryCountVerifier(
            expectedCounts,
            tableAssertions,
            tableNames,
            maxExecutionTimeMs
        );
        verifier.verify();
    }

}
