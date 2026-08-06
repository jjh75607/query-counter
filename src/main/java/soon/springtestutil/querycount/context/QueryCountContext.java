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

    private static volatile boolean active = false;

    private QueryCountContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 쿼리 기록이 활성화되었음을 표시합니다. DataSource를 감싸는 쪽에서 한 번 호출합니다.
     *
     * <p>활성 여부를 판단하려고 애플리케이션 컨텍스트를 참조하면 컨텍스트 로딩이 강제되므로,
     * 감싸기가 실제로 일어났는지를 신호로 씁니다.
     */
    public static void markActive() {
        active = true;
    }

    /**
     * 쿼리 기록이 활성화되어 있는지 반환합니다.
     */
    public static boolean isActive() {
        return active;
    }

    public static void addQuery(QueryType queryType, String query) {
        markActive();
        queries.get()
            .add(new QueryInfo(queryType, query));
    }

    public static void addQuery(QueryType queryType, String query, Long executionTimeMs) {
        markActive();
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