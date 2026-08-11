package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpectedCountTest {

    @DisplayName("exactly 는 같은 값에만 통과한다")
    @Test
    void exactlyMatchesOnlyTheSameCount() {
        // given
        ExpectedCount expected = ExpectedCount.exactly(3);

        // when & then
        assertThat(expected.matches(3)).isTrue();
        assertThat(expected.matches(2)).isFalse();
        assertThat(expected.matches(4)).isFalse();
    }

    @DisplayName("atMost 는 같거나 작은 값에 통과한다")
    @Test
    void atMostMatchesEqualOrFewer() {
        // given
        ExpectedCount expected = ExpectedCount.atMost(3);

        // when & then
        assertThat(expected.matches(0)).isTrue();
        assertThat(expected.matches(3)).isTrue();
        assertThat(expected.matches(4)).isFalse();
    }

    @DisplayName("실패 메시지에 비교 방식이 드러난다")
    @Test
    void describeShowsComparison() {
        // when & then
        assertThat(ExpectedCount.exactly(3).describe()).isEqualTo("expected 3");
        assertThat(ExpectedCount.atMost(3).describe()).isEqualTo("expected at most 3");
    }

    @DisplayName("음수는 기대값으로 받지 않는다")
    @Test
    void negativeCountIsRejected() {
        // when & then
        assertThatThrownBy(() -> ExpectedCount.atMost(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expected count must not be negative: -1");
    }

}
