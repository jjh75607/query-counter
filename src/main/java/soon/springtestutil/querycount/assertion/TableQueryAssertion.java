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
    private final Map<QueryType, Long> expectedCounts = new EnumMap<>(QueryType.class);
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
        expectedCounts.put(QueryType.SELECT, count);
        return this;
    }

    public TableQueryAssertion insert(long count) {
        expectedCounts.put(QueryType.INSERT, count);
        return this;
    }

    public TableQueryAssertion update(long count) {
        expectedCounts.put(QueryType.UPDATE, count);
        return this;
    }

    public TableQueryAssertion delete(long count) {
        expectedCounts.put(QueryType.DELETE, count);
        return this;
    }

    public TableQueryAssertion others(long count) {
        expectedCounts.put(QueryType.OTHERS, count);
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

    Map<QueryType, Long> getExpectedCounts() {
        return expectedCounts;
    }

    Long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

}
