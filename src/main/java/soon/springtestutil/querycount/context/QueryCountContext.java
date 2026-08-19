package soon.springtestutil.querycount.context;

import soon.springtestutil.querycount.NPlusOneCheck;
import soon.springtestutil.querycount.QueryLimit;
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

    private static final ThreadLocal<QueryLimit> queryLimit =
        ThreadLocal.withInitial(() -> QueryLimit.OFF);

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
     * 다른 스레드에서 모아 둔 쿼리를 이 스레드의 기록에 합칩니다.
     *
     * <p>테스트가 끝날 때 판정보다 먼저 부릅니다. 합치기 전에 판정하면 합친 의미가 없습니다.
     */
    public static void mergeOtherThreadQueries() {
        List<QueryInfo> drained = OtherThreadQueries.drain();
        if (!drained.isEmpty()) {
            queries.get().addAll(drained);
        }
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

    /**
     * Records the per-test query limit for the current test.
     *
     * <p>Internal wiring, carried the same way as the N+1 mode and for the same reason: the
     * setting lives in the application context and the listener that ends each test must not
     * touch that context.
     */
    public static void requestQueryLimit(QueryLimit limit) {
        queryLimit.set(limit);
    }

    /**
     * Returns the query limit for the current test, {@link QueryLimit#OFF} by default.
     */
    public static QueryLimit getQueryLimit() {
        return queryLimit.get();
    }

    public static List<QueryInfo> getQueries() {
        return new ArrayList<>(queries.get());
    }

    /**
     * 지금 스레드에 기록된 쿼리 개수입니다.
     *
     * <p>{@link #getQueries()} 는 복사본을 만들므로 쿼리마다 개수를 물을 때 쓸 수 없습니다.
     */
    public static int recordedCount() {
        return queries.get().size();
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
        OtherThreadQueries.clear();
        queryLimit.remove();
    }

}
