package soon.springtestutil.querycount.assertion;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import soon.springtestutil.querycount.NPlusOneCheck;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("어서션 없이 도는 전역 N+1 검사")
class NPlusOneWatchTest {

    private static final String SELECT_TEAM = "select * from team where id = ?";

    @BeforeEach
    @AfterEach
    void clearContext() {
        QueryCountContext.clear();
    }

    /**
     * 같은 SELECT 가 파라미터 값만 바꿔 두 번 나간 상태를 만듭니다. 이것이 N+1 입니다.
     */
    private void recordNPlusOne() {
        QueryCountContext.addQuery(QueryType.SELECT, SELECT_TEAM, 1L, List.of(List.of(1)));
        QueryCountContext.addQuery(QueryType.SELECT, SELECT_TEAM, 1L, List.of(List.of(2)));
    }

    @DisplayName("켜지 않으면 N+1 이 있어도 아무 일도 하지 않는다")
    @Test
    void runShouldDoNothingWhenOff() {
        // given
        recordNPlusOne();

        // when, then
        assertThatCode(NPlusOneWatch::run).doesNotThrowAnyException();
    }

    @DisplayName("보고 모드면 N+1 이 있어도 실패시키지 않는다")
    @Test
    void runShouldNotFailInReportMode() {
        // given
        recordNPlusOne();
        QueryCountContext.requestNPlusOneCheck(NPlusOneCheck.REPORT);

        // when, then
        assertThatCode(NPlusOneWatch::run).doesNotThrowAnyException();
    }

    @DisplayName("보고 모드는 찾은 것을 경고 로그로 남기고 SQL 을 함께 적는다")
    @Test
    void runShouldLogWarningInReportMode() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(NPlusOneWatch.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        recordNPlusOne();
        QueryCountContext.requestNPlusOneCheck(NPlusOneCheck.REPORT);

        // when
        NPlusOneWatch.run();
        logger.detachAppender(appender);

        // then - 실패시키지 않는 모드이므로 로그가 유일한 산출물이다
        assertThat(appender.list)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                    .contains("N+1 detected")
                    .contains(SELECT_TEAM);
            });
    }

    @DisplayName("실패 모드면 N+1 을 찾아 실패시키고 SQL 을 함께 낸다")
    @Test
    void runShouldFailWithSqlInMessage() {
        // given
        recordNPlusOne();
        QueryCountContext.requestNPlusOneCheck(NPlusOneCheck.FAIL);

        // when, then
        assertThatThrownBy(NPlusOneWatch::run)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("N+1 check failed")
            .hasMessageContaining(SELECT_TEAM)
            .hasMessageContaining("2 executions");
    }

    @DisplayName("파라미터까지 같은 반복은 N+1 이 아니라 실패시키지 않는다")
    @Test
    void runShouldIgnoreRepeatsWithSameParameters() {
        // given - 같은 값으로 두 번이면 중복 조회다
        QueryCountContext.addQuery(QueryType.SELECT, SELECT_TEAM, 1L, List.of(List.of(1)));
        QueryCountContext.addQuery(QueryType.SELECT, SELECT_TEAM, 1L, List.of(List.of(1)));
        QueryCountContext.requestNPlusOneCheck(NPlusOneCheck.FAIL);

        // when, then
        assertThatCode(NPlusOneWatch::run).doesNotThrowAnyException();
    }

    @DisplayName("기록된 쿼리가 없으면 실패 모드에서도 아무 일도 하지 않는다")
    @Test
    void runShouldDoNothingWithoutRecordedQueries() {
        // given
        QueryCountContext.requestNPlusOneCheck(NPlusOneCheck.FAIL);

        // when, then
        assertThatCode(NPlusOneWatch::run).doesNotThrowAnyException();
    }

}
