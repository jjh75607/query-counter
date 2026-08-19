package soon.springtestutil.querycount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("테스트별 쿼리 수 상한 값")
class QueryLimitTest {

    @DisplayName("둘 다 안 주면 아무 일도 하지 않는다")
    @Test
    void ofShouldReturnOffWhenNothingIsSet() {
        // given, when, then
        assertThat(QueryLimit.of(0, false)).isEqualTo(QueryLimit.OFF);
        assertThat(QueryLimit.OFF.isActive()).isFalse();
    }

    @DisplayName("보고만 켜도 테스트 끝에 할 일이 있다")
    @Test
    void ofShouldBeActiveWithReportOnly() {
        // given, when
        QueryLimit limit = QueryLimit.of(0, true);

        // then
        assertThat(limit.isActive()).isTrue();
        assertThat(limit.exceededBy(1000)).isFalse();
    }

    @DisplayName("상한을 넘었는지는 상한이 있을 때만 판정한다")
    @Test
    void exceededByShouldNeedALimit() {
        // given
        QueryLimit withLimit = QueryLimit.of(10, false);

        // when, then
        assertThat(withLimit.exceededBy(10)).isFalse();
        assertThat(withLimit.exceededBy(11)).isTrue();
        assertThat(QueryLimit.OFF.exceededBy(Long.MAX_VALUE)).isFalse();
    }

    @DisplayName("음수 상한은 0으로 본다")
    @Test
    void ofShouldTreatNegativeAsNoLimit() {
        // given, when
        QueryLimit limit = QueryLimit.of(-5, true);

        // then
        assertThat(limit.maxPerTest()).isZero();
        assertThat(limit.exceededBy(100)).isFalse();
    }

}
