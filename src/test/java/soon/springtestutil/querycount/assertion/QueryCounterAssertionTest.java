package soon.springtestutil.querycount.assertion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

@ExtendWith(QueryCountTestExtension.class)
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
            .hasMessage(
                """
                    [Test: soon.springtestutil.querycount.assertion.QueryCounterAssertionTest#verifyShouldFailWhenCountsDoNotMatch]Query count assertion failed:
                    QueryType.SELECT: expected 2, but was 1""");
    }

    @DisplayName("지정하지 않은 쿼리 타입은 검증하지 않는다.")
    @Test
    void verifyShouldIgnoreUnspecifiedTypes() {
        // given
        QueryCountContext.increment(QueryType.SELECT);
        QueryCountContext.increment(QueryType.INSERT);
        QueryCountContext.increment(QueryType.INSERT);
        QueryCountContext.increment(QueryType.UPDATE);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

    @DisplayName("여러 타입 중 일부만 지정하면, 지정한 타입만 검증한다.")
    @Test
    void verifyShouldCheckOnlySpecifiedTypes() {
        // given
        QueryCountContext.increment(QueryType.SELECT);
        QueryCountContext.increment(QueryType.INSERT);
        QueryCountContext.increment(QueryType.INSERT);
        QueryCountContext.increment(QueryType.UPDATE);

        // expected
        QueryCounterAssertion.assertCounts()
            .insert(2)
            .verify();
    }

    @Test
    @DisplayName("지정하지 않은 타입은 무시하고, 지정한 타입만 검증한다.")
    void verifyShouldOnlyCheckSpecifiedType() {
        // given
        QueryCountContext.increment(QueryType.SELECT);

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .insert(1)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessage(
                """
                    [Test: soon.springtestutil.querycount.assertion.QueryCounterAssertionTest#verifyShouldOnlyCheckSpecifiedType]Query count assertion failed:
                    QueryType.INSERT: expected 1, but was 0""");
    }

}