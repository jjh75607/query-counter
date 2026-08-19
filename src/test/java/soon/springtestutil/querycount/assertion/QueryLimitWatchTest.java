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
import soon.springtestutil.querycount.QueryLimit;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("테스트별 쿼리 수 상한 검사")
class QueryLimitWatchTest {

    private Logger logger;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        QueryCountContext.clear();
        logger = (Logger) LoggerFactory.getLogger(QueryLimitWatch.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        QueryCountContext.clear();
    }

    private void recordQueries(int count) {
        for (int i = 0; i < count; i++) {
            QueryCountContext.addQuery(QueryType.SELECT, "select " + i, 1L, List.of());
        }
    }

    @DisplayName("설정하지 않으면 쿼리가 많아도 아무 일도 하지 않는다")
    @Test
    void runShouldDoNothingWhenOff() {
        // given
        recordQueries(500);

        // when, then
        assertThatCode(QueryLimitWatch::run).doesNotThrowAnyException();
        assertThat(appender.list).isEmpty();
    }

    @DisplayName("상한을 넘으면 실패시키고 실제 개수와 상한을 함께 낸다")
    @Test
    void runShouldFailOverTheLimit() {
        // given
        recordQueries(12);
        QueryCountContext.requestQueryLimit(QueryLimit.of(10, false));

        // when, then
        assertThatThrownBy(QueryLimitWatch::run)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Query limit exceeded")
            .hasMessageContaining("12 queries ran")
            .hasMessageContaining("the limit is 10");
    }

    @DisplayName("상한과 같으면 통과한다")
    @Test
    void runShouldPassAtTheLimit() {
        // given
        recordQueries(10);
        QueryCountContext.requestQueryLimit(QueryLimit.of(10, false));

        // when, then
        assertThatCode(QueryLimitWatch::run).doesNotThrowAnyException();
    }

    @DisplayName("보고만 켜면 개수를 로그로 남기고 실패시키지 않는다")
    @Test
    void runShouldReportWithoutFailing() {
        // given - 상한을 무엇으로 잡을지 정하려고 쓰는 모드다
        recordQueries(37);
        QueryCountContext.requestQueryLimit(QueryLimit.of(0, true));

        // when
        QueryLimitWatch.run();

        // then
        assertThat(appender.list)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("37 queries");
            });
    }

    @DisplayName("보고를 켰는데 info 로그가 닫혀 있으면 그 사실을 알린다")
    @Test
    void runShouldWarnWhenReportIsInvisible() {
        // given - 테스트 설정에서 root 를 warn 으로 두는 프로젝트가 흔하다
        Level original = logger.getLevel();
        logger.setLevel(Level.WARN);
        try {
            recordQueries(5);
            QueryCountContext.requestQueryLimit(QueryLimit.of(0, true));

            // when
            QueryLimitWatch.run();

            // then - 켰는데 아무것도 안 보이는 상태를 그대로 두지 않는다
            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.WARN)
                .isNotEmpty()
                .allSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("logging.level.soon.springtestutil=info"));
        }
        finally {
            logger.setLevel(original);
        }
    }

    @DisplayName("보고와 상한을 함께 켜면 보고한 뒤 실패시킨다")
    @Test
    void runShouldReportBeforeFailing() {
        // given
        recordQueries(12);
        QueryCountContext.requestQueryLimit(QueryLimit.of(10, true));

        // when, then
        assertThatThrownBy(QueryLimitWatch::run).isInstanceOf(AssertionError.class);
        assertThat(appender.list).hasSize(1);
    }

}
