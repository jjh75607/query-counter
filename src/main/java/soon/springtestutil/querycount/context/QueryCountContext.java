package soon.springtestutil.querycount.context;

import soon.springtestutil.querycount.QueryType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects the queries executed on the current thread.
 *
 * <p>Uses {@link ThreadLocal} so that concurrently running tests do not see each
 * other's queries. Every method is static and this class cannot be instantiated.
 *
 * <p>The collected state is cleared before and after each test, and again when
 * an assertion completes. Tests that never assert must clear it themselves.
 */
public final class QueryCountContext {

    private static final ThreadLocal<List<QueryInfo>> queries = ThreadLocal
        .withInitial(ArrayList::new);

    private QueryCountContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void addQuery(QueryType queryType, String query) {
        queries.get()
            .add(new QueryInfo(queryType, query));
    }

    public static void addQuery(QueryType queryType, String query, Long executionTimeMs) {
        queries.get()
            .add(new QueryInfo(queryType, query, executionTimeMs));
    }

    public static List<QueryInfo> getQueries() {
        return new ArrayList<>(queries.get());
    }

    public static EnumMap<QueryType, Long> getQueryCounts() {
        return queries.get().stream()
            .collect(Collectors.groupingBy(
                QueryInfo::getQueryType,
                () -> new EnumMap<>(QueryType.class),
                Collectors.counting()
            ));
    }

    public static void clear() {
        queries.remove();
    }

}
