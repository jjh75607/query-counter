package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * verify() 를 잊었을 때 자동 검증이 잡아내는지 확인한다.
 *
 * <p>실제 실패는 테스트 메서드가 끝난 뒤에 일어나므로 여기서는 자동 검증의 진입점인
 * {@link QueryCounterAssertion#verifyPending()} 을 직접 호출해 검증한다.
 * 이 클래스에는 {@code @ExtendWith} 를 붙이지 않는다. 붙이면 확장이 먼저 검증해 버린다.
 */
@DisplayName("verify() 를 잊으면 자동 검증이 잡아낸다")
class VerifyPendingTest {

    @BeforeEach
    @AfterEach
    void reset() {
        QueryCounterAssertion.clearPending();
        QueryCountContext.clear();
    }

    @DisplayName("verify() 를 부르지 않은 어서션은 자동 검증에서 실패한다")
    @Test
    void pendingAssertionFailsOnAutoVerify() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(3); // verify() 를 부르지 않는다

        // when, then
        assertThatThrownBy(QueryCounterAssertion::verifyPending)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected 3, but was 1");
    }

    @DisplayName("verify() 를 이미 부른 어서션은 자동 검증에서 다시 검사하지 않는다")
    @Test
    void verifiedAssertionIsNotCheckedAgain() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(1).verify();

        // when, then
        assertThatCode(QueryCounterAssertion::verifyPending).doesNotThrowAnyException();
    }

    @DisplayName("검증할 어서션이 없으면 자동 검증은 아무 일도 하지 않는다")
    @Test
    void nothingHappensWhenNoAssertionWasCreated() {
        // given - 어서션을 만들지 않았다

        // when, then
        assertThatCode(QueryCounterAssertion::verifyPending).doesNotThrowAnyException();
    }

    @DisplayName("자동 검증은 남은 어서션을 모두 검사하므로 첫 번째가 통과해도 두 번째가 잡힌다")
    @Test
    void everyPendingAssertionIsChecked() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");
        QueryCounterAssertion.assertCounts().select(1);   // 통과할 조건
        QueryCounterAssertion.assertCounts().insert(5);   // 실패할 조건

        // when, then
        assertThatThrownBy(QueryCounterAssertion::verifyPending)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.INSERT: expected 5, but was 0");
    }

    @DisplayName("clearPending() 을 부르면 남아 있던 어서션이 버려진다")
    @Test
    void clearPendingDiscardsAssertions() {
        // given
        QueryCounterAssertion.assertCounts().select(99);

        // when
        QueryCounterAssertion.clearPending();

        // then
        assertThatCode(QueryCounterAssertion::verifyPending).doesNotThrowAnyException();
    }

}
