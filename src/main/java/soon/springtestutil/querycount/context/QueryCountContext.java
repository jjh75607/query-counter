package soon.springtestutil.querycount.context;

import soon.springtestutil.querycount.NPlusOneCheck;
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

    private static final ThreadLocal<NPlusOneCheck> nPlusOneCheck =
        ThreadLocal.withInitial(() -> NPlusOneCheck.OFF);

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

    public static void addQuery(
        QueryType queryType,
        String query,
        Long executionTimeMs,
        List<List<Object>> parameters
    ) {
        queries.get()
            .add(new QueryInfo(queryType, query, executionTimeMs, parameters));
    }

    /**
     * Records how the N+1 check should behave for the current test.
     *
     * <p>Internal wiring rather than part of the assertion API. The setting lives in the
     * application context, and the listener that ends each test must not touch that context,
     * so the recording side carries it here instead. A static field was tried before and
     * removed because it never reset between tests.
     */
    public static void requestNPlusOneCheck(NPlusOneCheck check) {
        nPlusOneCheck.set(check);
    }

    /**
     * Returns the N+1 check mode for the current test, {@link NPlusOneCheck#OFF} by default.
     */
    public static NPlusOneCheck getNPlusOneCheck() {
        return nPlusOneCheck.get();
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
        nPlusOneCheck.remove();
    }

}
