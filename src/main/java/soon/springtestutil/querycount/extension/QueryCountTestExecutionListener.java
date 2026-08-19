package soon.springtestutil.querycount.extension;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.assertion.NPlusOneWatch;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.assertion.QueryLimitWatch;
import soon.springtestutil.querycount.context.QueryCountContext;

/**
 * Wires query-counter into every Spring test without requiring an annotation.
 *
 * <p>Registered through {@code META-INF/spring.factories}, so the Spring TestContext
 * framework picks it up as soon as this library is on the test classpath. No
 * {@code @ExtendWith} is needed.
 *
 * <p>Responsibilities:
 *
 * <ul>
 * <li>Isolate tests by clearing the recorded queries before and after each test method.
 * <li>Record the test class and method name so that assertion failures identify the test.
 * <li>Verify assertions that were created but never verified, so a forgotten
 *     {@code verify()} cannot make a test pass silently.
 * </ul>
 *
 * <p>This listener never touches the application context. Resolving whether the library
 * is enabled through {@code testContext.getApplicationContext()} would force the context
 * to load, which is unacceptable for a listener that runs for every Spring test. Clearing
 * thread local state is harmless whether the library is enabled or not, so it is done
 * unconditionally.
 */
public class QueryCountTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TestContextHolder.setContext(
            testContext.getTestClass().getName(),
            testContext.getTestMethod().getName()
        );

        QueryCounterAssertion.clearPending();
        QueryCountContext.clear();
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        try {
            // 테스트가 다른 이유로 이미 실패했으면 자동 검증을 건너뛴다.
            // 그러지 않으면 진짜 실패 위에 쿼리 카운트 실패가 덮여 원인이 가려진다.
            if (testContext.getTestException() == null) {
                QueryCounterAssertion.verifyPending();
                NPlusOneWatch.run();
                QueryLimitWatch.run();
            }
        }
        finally {
            QueryCounterAssertion.clearPending();
            TestContextHolder.clearContext();
            QueryCountContext.clear();
        }
    }

}
