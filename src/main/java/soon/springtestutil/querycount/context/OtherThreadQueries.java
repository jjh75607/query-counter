package soon.springtestutil.querycount.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 테스트 스레드가 아닌 곳에서 나간 쿼리를 잠시 모아 두는 자리입니다.
 *
 * <p>HTTP 로 서버를 거치는 테스트는 쿼리가 톰캣 워커 스레드에서 나갑니다. 기록은
 * {@link QueryCountContext} 의 {@code ThreadLocal} 에 담기므로 테스트 스레드에서는 그것이
 * 보이지 않습니다. 그래서 그런 쿼리는 여기 모아 두고, 테스트가 끝날 때 리스너가 가져갑니다.
 *
 * <p><b>static 가변 상태입니다.</b> 예전에 static 플래그가 리셋되지 않아 테스트 불가능해져
 * 걷어낸 적이 있으므로, 여기서는 규율을 셋으로 정해 둡니다. 비어 있는 것이 안전한 기본이고,
 * 테스트 시작과 끝 양쪽에서 비우고, 그 리셋을 테스트가 고정합니다.
 *
 * <p>JVM 하나에서 테스트가 한 번에 하나만 도는 것을 전제로 합니다. JUnit 은 기본이 순차이고
 * Gradle 의 {@code maxParallelForks} 는 JVM 을 따로 띄우므로 그 전제가 성립합니다. JVM 안에서
 * 테스트를 병렬로 돌리면 다른 테스트의 쿼리가 섞입니다.
 */
public final class OtherThreadQueries {

    private static final Queue<QueryInfo> queries = new ConcurrentLinkedQueue<>();

    private OtherThreadQueries() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 테스트 스레드가 아닌 곳에서 나간 쿼리를 담습니다.
     */
    public static void add(QueryInfo query) {
        queries.add(query);
    }

    /**
     * 모아 둔 것을 가져가고 비웁니다. 테스트가 끝날 때 한 번 부릅니다.
     */
    public static List<QueryInfo> drain() {
        List<QueryInfo> drained = new ArrayList<>();
        QueryInfo query;
        while ((query = queries.poll()) != null) {
            drained.add(query);
        }
        return drained;
    }

    /**
     * 가져가지 않고 버립니다.
     */
    public static void clear() {
        queries.clear();
    }

    /**
     * 지금 모여 있는 개수입니다. 리셋을 검증하는 테스트가 씁니다.
     */
    public static int size() {
        return queries.size();
    }

}
