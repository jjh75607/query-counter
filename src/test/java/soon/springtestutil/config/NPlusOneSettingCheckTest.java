package soon.springtestutil.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("검사를 켰지만 카운팅을 켜지 않은 구성 안내")
class NPlusOneSettingCheckTest {

    // DataSource 도 AutoConfig 도 넣지 않는다. 이 확인은 Environment 만 읽으므로 필요하지 않고,
    // Boot 4 에서 자동 설정 클래스의 패키지가 옮겨져 버전마다 다른 import 가 된다.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(NPlusOneSettingCheck.class);

    private Logger logger;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(NPlusOneSettingCheck.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @DisplayName("검사만 켜고 카운팅을 켜지 않으면 경고한다")
    @Test
    void shouldWarnWhenCountingIsNotEnabled() {
        // given, when
        runner.withPropertyValues("query-counter.n-plus-one.enabled=true")
            .run(context -> {
                // then
                assertThat(context).hasSingleBean(NPlusOneSettingCheck.class);
                assertThat(appender.list)
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage())
                            .contains("query-counter.enabled");
                    });
            });
    }

    @DisplayName("둘 다 켜면 경고하지 않는다")
    @Test
    void shouldStaySilentWhenBothAreEnabled() {
        // given, when
        runner.withPropertyValues(
            "query-counter.enabled=true",
            "query-counter.n-plus-one.enabled=true"
        ).run(context -> {
            // then
            assertThat(context).hasSingleBean(NPlusOneSettingCheck.class);
            assertThat(appender.list).isEmpty();
        });
    }

    @DisplayName("검사를 켜지 않은 프로젝트에는 이 빈이 만들어지지 않는다")
    @Test
    void shouldNotBeCreatedWhenCheckIsOff() {
        // given, when
        runner.withPropertyValues("query-counter.enabled=true")
            .run(context -> {
                // then
                assertThat(context).doesNotHaveBean(NPlusOneSettingCheck.class);
                assertThat(appender.list).isEmpty();
            });
    }

}
