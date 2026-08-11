package soon.springtestutil.querycount.assertion;

import org.springframework.util.StringUtils;
import soon.springtestutil.querycount.QueryType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the assertions declared for a single table.
 */
public class TableQueryAssertion {

    private final QueryCounterAssertion parent;
    private final String tableName;
    private final Map<QueryType, ExpectedCount> expectedCounts = new EnumMap<>(QueryType.class);
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
        return select(ExpectedCount.exactly(count));
    }

    public TableQueryAssertion select(ExpectedCount expected) {
        expectedCounts.put(QueryType.SELECT, expected);
        return this;
    }

    public TableQueryAssertion insert(long count) {
        return insert(ExpectedCount.exactly(count));
    }

    public TableQueryAssertion insert(ExpectedCount expected) {
        expectedCounts.put(QueryType.INSERT, expected);
        return this;
    }

    public TableQueryAssertion update(long count) {
        return update(ExpectedCount.exactly(count));
    }

    public TableQueryAssertion update(ExpectedCount expected) {
        expectedCounts.put(QueryType.UPDATE, expected);
        return this;
    }

    public TableQueryAssertion delete(long count) {
        return delete(ExpectedCount.exactly(count));
    }

    public TableQueryAssertion delete(ExpectedCount expected) {
        expectedCounts.put(QueryType.DELETE, expected);
        return this;
    }

    public TableQueryAssertion others(long count) {
        return others(ExpectedCount.exactly(count));
    }

    public TableQueryAssertion others(ExpectedCount expected) {
        expectedCounts.put(QueryType.OTHERS, expected);
        return this;
    }

    public TableQueryAssertion maxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        return this;
    }

    /**
     * Starts assertions for another table.
     */
    public TableQueryAssertion forTable(String tableName) {
        return parent.forTable(tableName);
    }

    /**
     * Restricts the assertion to the given tables, counting them together.
     */
    public QueryCounterAssertion forTables(String... tableNames) {
        return parent.forTables(tableNames);
    }

    /**
     * Runs every assertion declared so far and reports all mismatches at once.
     */
    public void verify() {
        parent.verify();
    }

    String getTableName() {
        return tableName;
    }

    Map<QueryType, ExpectedCount> getExpectedCounts() {
        return expectedCounts;
    }

    Long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

}
