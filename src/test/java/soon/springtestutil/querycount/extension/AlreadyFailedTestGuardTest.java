package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.TestContext;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 테스트가 다른 이유로 이미 실패했으면 자동 검증이 그 실패를 덮지 않아야 한다.
 */
@DisplayName("이미 실패한 테스트에서는 자동 검증을 건너뛴다")
class AlreadyFailedTestGuardTest {

    private final QueryCountTestExecutionListener listener = new QueryCountTestExecutionListener();

    @BeforeEach
    @AfterEach
    void reset() {
        QueryCounterAssertion.clearPending();
        QueryCountContext.clear();
    }

    @DisplayName("테스트가 실패하지 않았으면 남은 어서션을 검증한다")
    @Test
    void pendingAssertionIsVerifiedWhenTestPassed() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3);

        TestContext testContext = Mockito.mock(TestContext.class);
        Mockito.when(testContext.getTestException()).thenReturn(null);

        // when, then
        assertThatThrownBy(() -> listener.afterTestMethod(testContext))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected 3, but was 1");
    }

    @DisplayName("테스트가 이미 실패했으면 남은 어서션을 검증하지 않아 원래 실패가 가려지지 않는다")
    @Test
    void pendingAssertionIsSkippedWhenTestAlreadyFailed() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3); // 검증하면 실패할 조건

        TestContext testContext = Mockito.mock(TestContext.class);
        Mockito.when(testContext.getTestException())
            .thenReturn(new IllegalStateException("테스트 본문에서 이미 터진 예외"));

        // when, then
        assertThatCode(() -> listener.afterTestMethod(testContext)).doesNotThrowAnyException();
    }

    @DisplayName("건너뛴 경우에도 남은 어서션과 기록된 쿼리는 정리된다")
    @Test
    void stateIsClearedEvenWhenSkipped() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3);

        TestContext testContext = Mockito.mock(TestContext.class);
        Mockito.when(testContext.getTestException())
            .thenReturn(new IllegalStateException("이미 터진 예외"));

        // when
        listener.afterTestMethod(testContext);

        // then - 다음 테스트로 상태가 넘어가지 않는다
        assertThatCode(QueryCounterAssertion::verifyPending).doesNotThrowAnyException();
    }

}
