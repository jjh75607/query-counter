package soon.springtestutil.querycount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("N+1 검사 모드")
class NPlusOneCheckTest {

    @DisplayName("켜지 않으면 검사하지 않는다")
    @Test
    void ofShouldReturnOffWhenNotEnabled() {
        // given, when, then
        assertThat(NPlusOneCheck.of(false, false)).isEqualTo(NPlusOneCheck.OFF);
        assertThat(NPlusOneCheck.of(false, true)).isEqualTo(NPlusOneCheck.OFF);
    }

    @DisplayName("켜기만 하면 보고만 하고 실패시키지 않는다")
    @Test
    void ofShouldReturnReportWhenEnabledWithoutFail() {
        // given, when, then
        assertThat(NPlusOneCheck.of(true, false)).isEqualTo(NPlusOneCheck.REPORT);
    }

    @DisplayName("fail 을 함께 켜면 실패시킨다")
    @Test
    void ofShouldReturnFailWhenBothEnabled() {
        // given, when, then
        assertThat(NPlusOneCheck.of(true, true)).isEqualTo(NPlusOneCheck.FAIL);
    }

}
