package soon.springtestutil.querycount.assertion;

import lombok.extern.slf4j.Slf4j;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.NPlusOneCheck;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.List;

/**
 * Runs the N+1 check for a test that never asked for it.
 *
 * <p>Called at the end of every test by the listener and the extension. It does nothing
 * unless {@code query-counter.n-plus-one.enabled=true}, and nothing when the test recorded
 * no queries.
 *
 * <p>Assertions written by hand are unaffected. A chain that already called
 * {@code noNPlusOne()} is verified first and throws before this runs, so the same finding
 * is never reported twice.
 */
@Slf4j
public final class NPlusOneWatch {

    private NPlusOneWatch() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks the queries recorded for the current test and reports or fails.
     *
     * @throws AssertionError when the mode is {@link NPlusOneCheck#FAIL} and an N+1 is found
     */
    public static void run() {
        NPlusOneCheck check = QueryCountContext.getNPlusOneCheck();
        if (check == NPlusOneCheck.OFF) {
            return;
        }

        List<NPlusOneDetector.Finding> findings = NPlusOneDetector.detect(QueryCountContext.getQueries());
        if (findings.isEmpty()) {
            return;
        }

        String message = TestContextHolder.getContextInfo()
            + NPlusOneDetector.format(findings, headline(check, findings.size()));

        if (check == NPlusOneCheck.FAIL) {
            throw new AssertionError(message);
        }
        log.warn("{}", message);
    }

    private static String headline(NPlusOneCheck check, int findingCount) {
        String verb = check == NPlusOneCheck.FAIL ? "N+1 check failed" : "N+1 detected";
        return String.format("%s: %d query %s ran with different parameter values",
            verb, findingCount, findingCount == 1 ? "shape" : "shapes");
    }

}
