package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.assertion.NPlusOneWatch;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

/**
 * JUnit extension that isolates recorded queries between tests.
 *
 * <p><strong>Spring tests do not need this.</strong>
 * {@link QueryCountTestExecutionListener} is registered through
 * {@code META-INF/spring.factories} and does the same work for every Spring test, so
 * {@code @ExtendWith(QueryCountTestExtension.class)} can be omitted.
 *
 * <p>It stays useful for tests that do not load a Spring test context, where no
 * {@code TestExecutionListener} runs.
 *
 * <p>Running both is safe. Whichever runs first verifies the pending assertions and
 * clears the recorded queries; the other then finds nothing left to do.
 */
public class QueryCountTestExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        TestContextHolder.setContext(
            context.getRequiredTestClass().getName(),
            context.getRequiredTestMethod().getName()
        );

        QueryCounterAssertion.clearPending();
        QueryCountContext.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            // 테스트가 다른 이유로 이미 실패했으면 자동 검증을 건너뛴다.
            // 그러지 않으면 진짜 실패 위에 쿼리 카운트 실패가 덮여 원인이 가려진다.
            if (context.getExecutionException().isEmpty()) {
                QueryCounterAssertion.verifyPending();
                NPlusOneWatch.run();
            }
        }
        finally {
            QueryCounterAssertion.clearPending();
            TestContextHolder.clearContext();
            QueryCountContext.clear();
        }
    }

}
