package soon.springtestutil.querycount.context;

import soon.springtestutil.querycount.QueryType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 현재 스레드에서 실행된 쿼리 정보를 저장하고 관리하는 유틸리티 클래스입니다. 이 클래스는 {@link ThreadLocal}을 사용하여 각 스레드별로 쿼리 정보를 격리합니다.
 * 모든 메서드는 정적이며, 인스턴스화할 수 없습니다.
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