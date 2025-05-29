package soon.springtestutil.querycount;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueryCounterAssertionTest {

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    @DisplayName("쿼리 횟수가 예상과 일치하면 예외를 발생하지 않는다.")
    @Test
    void verifyShouldPassWhenCountMatch() {
        // given
        QueryCountContext.increment(QueryType.SELECT);
        QueryCountContext.increment(QueryType.SELECT);
        QueryCountContext.increment(QueryType.INSERT);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(2)
            .insert(1)
            .verify();
    }

    @DisplayName("쿼리 횟수가 예상과 일치하지 않는다면 예외가 발생한다.")
    @Test
    void verifyShouldFailWhenCountsDoNotMatch() {
        // given
        QueryCountContext.increment(QueryType.SELECT);
        QueryCountContext.increment(QueryType.INSERT);

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(2)
            .insert(1)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Query count assertion failed")
            .hasMessageContaining("QueryType.SELECT: expected 2, but was 1");
    }

    @Test
    @DisplayName("쿼리 횟수가 설정되지 않은 경우 기본값인 0으로 검증한다.")
    void verifyShouldDefaultToZeroForUnsetQueryTypes() {
        // given
        QueryCountContext.increment(QueryType.SELECT);

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .insert(1)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.INSERT: expected 1, but was 0");
    }

}