package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.TestContext;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 리스너 콜백을 직접 호출해 검증한다.
 *
 * <p>테스트 간 격리는 리스너 콜백이 하는 일이므로, 테스트 두 개를 순서대로 실행해
 * 간접적으로 확인하지 않는다. 그러면 테스트가 서로 의존하게 되고 순서가 바뀌면
 * 무의미하게 통과한다. 콜백을 직접 부르면 각 콜백을 따로 검증할 수 있다.
 */
@DisplayName("리스너 콜백 동작")
class QueryCountTestExecutionListenerCallbackTest {

    private final QueryCountTestExecutionListener listener = new QueryCountTestExecutionListener();

    @BeforeEach
    @AfterEach
    void reset() {
        QueryCounterAssertion.clearPending();
        QueryCountContext.clear();
        TestContextHolder.clearContext();
    }

    private TestContext testContextWithException(Throwable exception) throws NoSuchMethodException {
        TestContext testContext = Mockito.mock(TestContext.class);
        Mockito.when(testContext.getTestException()).thenReturn(exception);
        return testContext;
    }

    private TestContext testContextForMethod(String methodName) throws NoSuchMethodException {
        TestContext testContext = Mockito.mock(TestContext.class);
        Mockito.<Class<?>>when(testContext.getTestClass()).thenReturn(getClass());
        Mockito.when(testContext.getTestMethod()).thenReturn(getClass().getDeclaredMethod(methodName));
        return testContext;
    }

    @DisplayName("beforeTestMethod 는 앞선 테스트가 남긴 쿼리를 비운다")
    @Test
    void beforeTestMethodClearsRecordedQueries() throws Exception {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        assertThat(QueryCountContext.getQueries()).isNotEmpty();

        // when
        listener.beforeTestMethod(testContextForMethod("beforeTestMethodClearsRecordedQueries"));

        // then
        assertThat(QueryCountContext.getQueries()).isEmpty();
    }

    @DisplayName("beforeTestMethod 는 테스트 클래스와 메서드 이름을 기록한다")
    @Test
    void beforeTestMethodRecordsTestName() throws Exception {
        // when
        listener.beforeTestMethod(testContextForMethod("beforeTestMethodRecordsTestName"));

        // then
        assertThat(TestContextHolder.getContextInfo())
            .contains(getClass().getName())
            .contains("beforeTestMethodRecordsTestName");
    }

    @DisplayName("afterTestMethod 는 실행된 쿼리와 테스트 이름을 비운다")
    @Test
    void afterTestMethodClearsState() throws Exception {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        TestContextHolder.setContext("SomeClass", "someMethod");

        // when
        listener.afterTestMethod(testContextWithException(null));

        // then
        assertThat(QueryCountContext.getQueries()).isEmpty();
        assertThat(TestContextHolder.getContextInfo()).isEmpty();
    }

    @DisplayName("테스트가 실패하지 않았으면 남은 어서션을 검증한다")
    @Test
    void pendingAssertionIsVerifiedWhenTestPassed() throws Exception {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3);

        // when, then
        assertThatThrownBy(() -> listener.afterTestMethod(testContextWithException(null)))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected 3, but was 1");
    }

    @DisplayName("테스트가 이미 실패했으면 남은 어서션을 검증하지 않아 원래 실패가 가려지지 않는다")
    @Test
    void pendingAssertionIsSkippedWhenTestAlreadyFailed() throws Exception {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3); // 검증하면 실패할 조건

        TestContext testContext =
            testContextWithException(new IllegalStateException("테스트 본문에서 이미 터진 예외"));

        // when, then
        assertThatCode(() -> listener.afterTestMethod(testContext)).doesNotThrowAnyException();
    }

    @DisplayName("자동 검증을 건너뛴 경우에도 남은 어서션은 정리된다")
    @Test
    void pendingIsClearedEvenWhenSkipped() throws Exception {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3);

        // when
        listener.afterTestMethod(
            testContextWithException(new IllegalStateException("이미 터진 예외")));

        // then
        assertThatCode(QueryCounterAssertion::verifyPending).doesNotThrowAnyException();
    }

}
