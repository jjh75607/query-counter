package soon.springtestutil.querycount;

import java.util.EnumMap;
import java.util.Map;

/**
 * 현재 스레드에서 실행된 쿼리 유형별 횟수를 저장하고 관리하는 유틸리티 클래스입니다.
 * 이 클래스는 {@link ThreadLocal}을 사용하여 각 스레드별로 쿼리 카운트를 격리합니다.
 * 모든 메서드는 정적이며, 인스턴스화할 수 없습니다.
 */
public final class QueryCountContext {

    private static final ThreadLocal<Map<QueryType, Long>> queryCounts = ThreadLocal.withInitial(
        () -> new EnumMap<>(QueryType.class)
    );

    private QueryCountContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void increment(QueryType queryType) {
        queryCounts.get()
            .compute(queryType, (k, v) -> (v == null) ? 1L : v + 1L);
    }

    public static EnumMap<QueryType, Long> getQueryCounts() {
        return new EnumMap<>(queryCounts.get());
    }

    public static void clear() {
        queryCounts.remove();
    }

}