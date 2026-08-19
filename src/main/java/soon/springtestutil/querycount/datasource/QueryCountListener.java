package soon.springtestutil.querycount.datasource;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.NPlusOneCheck;
import soon.springtestutil.querycount.QueryLimit;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.OtherThreadQueries;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class QueryCountListener implements QueryExecutionListener {

    private static final ConcurrentMap<String, QueryType> queryTypeCache = new ConcurrentHashMap<>();

    private final NPlusOneCheck nPlusOneCheck;

    private final boolean collectOtherThreads;

    private final QueryLimit queryLimit;

    public QueryCountListener() {
        this(NPlusOneCheck.OFF, false, QueryLimit.OFF);
    }

    public QueryCountListener(NPlusOneCheck nPlusOneCheck) {
        this(nPlusOneCheck, false, QueryLimit.OFF);
    }

    public QueryCountListener(NPlusOneCheck nPlusOneCheck, boolean collectOtherThreads) {
        this(nPlusOneCheck, collectOtherThreads, QueryLimit.OFF);
    }

    public QueryCountListener(
        NPlusOneCheck nPlusOneCheck,
        boolean collectOtherThreads,
        QueryLimit queryLimit
    ) {
        this.nPlusOneCheck = nPlusOneCheck;
        this.collectOtherThreads = collectOtherThreads;
        this.queryLimit = queryLimit;
    }

    /**
     * 아무도 비우지 않는 기록을 여기까지만 담습니다.
     *
     * <p>테스트 스레드가 아닌 곳의 기록은 `other-threads` 를 켜지 않으면 읽을 수도 없고
     * 비워지지도 않습니다. 그런 기록이 무한히 쌓이지 않게 상한을 둡니다. 실제로 쓰이는 경로에
     * 닿지 않을 만큼 넉넉하게 잡습니다.
     */
    static final int UNCOLLECTED_CAP = 10_000;

    private static final AtomicBoolean warnedAboutUncollected = new AtomicBoolean();

    /**
     * 기록을 어디에 담을지 고릅니다.
     *
     * <p>켜져 있고 테스트 스레드가 아니면 공용 수집소에 담습니다. 그 밖에는 지금까지처럼 이
     * 스레드의 기록에 담습니다.
     *
     * <p><b>꺼져 있을 때의 동작을 바꾸지 않습니다.</b> 테스트 스레드가 아닌 곳의 기록을 버리는
     * 쪽으로 바꿔 봤더니 리스너를 직접 부르는 기존 테스트 다섯 개가 깨졌다. 활성화하지 않은
     * 사용자에게 영향을 주지 않는 것이 이 라이브러리의 첫 성질이므로 그 방향은 버렸다.
     */
    static void route(
        boolean collectOtherThreads,
        QueryType queryType,
        String sql,
        Long elapsedMs,
        List<List<Object>> parameters
    ) {
        if (collectOtherThreads && !TestContextHolder.isInTest()) {
            OtherThreadQueries.add(new soon.springtestutil.querycount.context.QueryInfo(
                queryType, sql, elapsedMs, parameters));
            return;
        }
        if (!TestContextHolder.isInTest() && QueryCountContext.recordedCount() >= UNCOLLECTED_CAP) {
            warnAboutUncollected();
            return;
        }
        QueryCountContext.addQuery(queryType, sql, elapsedMs, parameters);
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // 설정은 애플리케이션 컨텍스트에 있고 테스트를 끝내는 리스너는 그 컨텍스트를 만질 수
        // 없다. 그래서 기록하는 이쪽이 모드를 같은 ThreadLocal 에 실어 나른다.
        QueryCountContext.requestNPlusOneCheck(nPlusOneCheck);
        QueryCountContext.requestQueryLimit(queryLimit);

        Long elapsedMs = null;
        try {
            if (execInfo != null) {
                elapsedMs = execInfo.getElapsedTime();
            }
        } catch (Exception e) {
            log.debug("Failed to get elapsed time from ExecutionInfo: {}", e.getMessage());
        }

        for (QueryInfo queryInfo : queryInfoList) {
            String sql = queryInfo.getQuery();
            if (sql != null && !sql.trim().isEmpty()) {
                try {
                    QueryType queryType = queryTypeCache.computeIfAbsent(sql, QueryType::from);
                    route(collectOtherThreads, queryType, sql, elapsedMs, extractParameters(queryInfo));
                } catch (IllegalArgumentException e) {
                    String contextInfo = TestContextHolder.getContextInfo();
                    log.warn("{}Cannot determine query type for: [{}]. Error: {}",
                        contextInfo,
                        sql,
                        e.getMessage()
                    );
                }
            }
        }
    }

    /**
     * 상한에 닿았다는 것을 한 번만 알립니다.
     *
     * <p>한 번 알렸으면 그대로 두는 것이 맞는 값이라, 테스트 사이에 리셋해야 했던 예전의
     * static 플래그와 성질이 다릅니다.
     */
    private static void warnAboutUncollected() {
        if (!warnedAboutUncollected.compareAndSet(false, true)) {
            return;
        }
        log.warn("{} queries piled up on a thread that is not a test thread, and nothing clears "
                + "them. Later ones are dropped. If these come from a server handling requests "
                + "for an acceptance test, set query-counter.other-threads.enabled=true to count "
                + "them instead.", UNCOLLECTED_CAP);
    }

    /**
     * 바인딩된 파라미터 값을 꺼냅니다. setter 인자가 (인덱스, 값) 이라 두 번째가 값입니다.
     *
     * <p>세트가 여럿이면 배치입니다. 이 경우 분석을 멈추지 않고 세트를 그대로 넘깁니다.
     */
    private List<List<Object>> extractParameters(QueryInfo queryInfo) {
        List<List<ParameterSetOperation>> parametersList = queryInfo.getParametersList();
        if (parametersList == null || parametersList.isEmpty()) {
            return List.of();
        }

        List<List<Object>> sets = new ArrayList<>();
        for (List<ParameterSetOperation> operations : parametersList) {
            List<Object> values = new ArrayList<>();
            for (ParameterSetOperation operation : operations) {
                Object[] args = operation.getArgs();
                values.add(args != null && args.length > 1 ? args[1] : null);
            }
            sets.add(values);
        }
        return sets;
    }

}
