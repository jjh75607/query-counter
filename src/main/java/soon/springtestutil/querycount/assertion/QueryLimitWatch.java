package soon.springtestutil.querycount.assertion;

import lombok.extern.slf4j.Slf4j;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryLimit;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies the per-test query limit, and reports counts when asked.
 *
 * <p>Called at the end of every test by the listener and the extension, after the assertions
 * written by hand and after the N+1 check. Those say something more specific, so they should
 * fail first.
 */
@Slf4j
public final class QueryLimitWatch {

    /**
     * 로그 수준 안내를 한 번만 내기 위한 표시입니다.
     *
     * <p>테스트 사이에 되돌릴 필요가 없는 값입니다. 한 번 알렸으면 그대로 두는 것이 맞는
     * 동작이므로, 리셋되지 않아 문제가 됐던 예전의 static 플래그와 성질이 다릅니다.
     */
    private static final AtomicBoolean warnedAboutLogLevel = new AtomicBoolean();

    private QueryLimitWatch() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reports the count and fails when the limit is exceeded.
     *
     * @throws AssertionError when the test ran more queries than the limit allows
     */
    public static void run() {
        QueryLimit limit = QueryCountContext.getQueryLimit();
        if (!limit.isActive()) {
            return;
        }

        EnumMap<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        if (limit.report()) {
            // 상한을 무엇으로 잡을지 정하려면 지금 몇 개가 나가는지 알아야 한다.
            warnIfReportIsInvisible();
            log.info("{}{} queries: {}", TestContextHolder.getContextInfo(), total, counts);
        }

        if (limit.exceededBy(total)) {
            throw new AssertionError(String.format(
                "%sQuery limit exceeded: %d queries ran, the limit is %d%n  %s",
                TestContextHolder.getContextInfo(), total, limit.maxPerTest(), counts));
        }
    }

    /**
     * 보고를 켰는데 이 로거의 info 가 닫혀 있으면 알립니다.
     *
     * <p>테스트 설정에서 {@code logging.level.root: warn} 을 두는 프로젝트가 흔합니다. 그러면
     * 보고가 걸러져 아무것도 안 보이는데, <b>켰는데 아무 일도 없는 것으로 읽힙니다.</b>
     */
    private static void warnIfReportIsInvisible() {
        if (log.isInfoEnabled() || !warnedAboutLogLevel.compareAndSet(false, true)) {
            return;
        }
        log.warn("query-counter.max-queries.report is on but info logging is off for {}, "
                + "so nothing will be printed. Set logging.level.soon.springtestutil=info.",
            QueryLimitWatch.class.getName());
    }

}
