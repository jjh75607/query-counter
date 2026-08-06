package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("기록된 쿼리가 없을 때의 안내")
@ExtendWith(QueryCountTestExtension.class)
class NoQueryRecordedHintTest {

    @DisplayName("기록된 쿼리가 없어 검증이 실패하면 활성화 설정을 확인하라는 안내가 함께 나온다")
    @Test
    void hintIsAppendedWhenNothingWasRecorded() {
        // given - 쿼리를 하나도 기록하지 않는다. 활성화를 잊은 상황과 같다

        // when, then
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(1)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("query-counter.enabled=true");
    }

    @DisplayName("쿼리가 기록되었는데 검증이 실패하면 활성화 안내는 나오지 않는다")
    @Test
    void hintIsNotAppendedWhenQueryWasRecorded() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "select 1 from member");

        // when, then
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(3)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageNotContaining("query-counter.enabled=true");
    }

    @DisplayName("기록된 쿼리가 없고 기대값도 0이면 검증을 통과하고 안내도 나오지 않는다")
    @Test
    void noHintWhenAssertionPasses() {
        // given - 기록도 없고 기대도 0이다

        // when, then
        assertThatCode(() -> QueryCounterAssertion.assertCounts()
            .select(0)
            .verify())
            .doesNotThrowAnyException();
    }

}
