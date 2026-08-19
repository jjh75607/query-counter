package soon.springtestutil.querycount.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.OtherThreadQueries;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("기록을 어디에 담을지 고르는 규칙")
class QueryCountListenerRouteTest {

    @BeforeEach
    @AfterEach
    void clearAll() {
        TestContextHolder.clearContext();
        QueryCountContext.clear();
        OtherThreadQueries.clear();
    }

    private void record(boolean collectOtherThreads) {
        QueryCountListener.route(collectOtherThreads, QueryType.SELECT, "select 1", 1L, List.of());
    }

    @DisplayName("테스트 스레드에서 나간 것은 그 스레드의 기록에 담긴다")
    @Test
    void routeShouldRecordOnTestThread() {
        // given
        TestContextHolder.setContext("SomeTest", "someMethod");

        // when
        record(false);

        // then
        assertThat(QueryCountContext.getQueries()).hasSize(1);
        assertThat(OtherThreadQueries.size()).isZero();
    }

    @DisplayName("테스트 스레드가 아니고 켜져 있으면 수집소에 담긴다")
    @Test
    void routeShouldCollectOffTestThreadWhenEnabled() {
        // given - 컨텍스트가 비어 있으면 테스트 스레드가 아니다

        // when
        record(true);

        // then
        assertThat(QueryCountContext.getQueries()).isEmpty();
        assertThat(OtherThreadQueries.size()).isEqualTo(1);
    }

    @DisplayName("테스트 스레드가 아닌 기록은 상한까지만 담는다")
    @Test
    void routeShouldStopAtTheCapOffTestThread() {
        // given - 아무도 비우지 않는 기록이라 무한히 쌓이면 안 된다
        for (int i = 0; i < QueryCountListener.UNCOLLECTED_CAP; i++) {
            record(false);
        }

        // when
        record(false);

        // then
        assertThat(QueryCountContext.recordedCount()).isEqualTo(QueryCountListener.UNCOLLECTED_CAP);
    }

    @DisplayName("테스트 스레드에서는 상한을 넘겨도 계속 담는다")
    @Test
    void routeShouldIgnoreTheCapOnTestThread() {
        // given - 테스트 끝에 비워지는 기록이라 쌓일 일이 없다
        TestContextHolder.setContext("SomeTest", "someMethod");
        for (int i = 0; i < QueryCountListener.UNCOLLECTED_CAP; i++) {
            record(false);
        }

        // when
        record(false);

        // then
        assertThat(QueryCountContext.recordedCount())
            .isEqualTo(QueryCountListener.UNCOLLECTED_CAP + 1);
    }

    @DisplayName("꺼져 있으면 테스트 스레드가 아니어도 지금까지처럼 이 스레드에 담는다")
    @Test
    void routeShouldKeepOldBehaviourWhenDisabled() {
        // given, when
        record(false);

        // then - 활성화하지 않은 사용자에게 영향을 주지 않는 것이 먼저다
        assertThat(QueryCountContext.getQueries()).hasSize(1);
        assertThat(OtherThreadQueries.size()).isZero();
    }

}
