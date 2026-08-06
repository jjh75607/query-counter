package soon.springtestutil.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SQL 로깅은 프로퍼티로만 켜진다")
class QueryLoggingTest {

    private Logger targetLogger;
    private ListAppender<ILoggingEvent> appender;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
        .withUserConfiguration(AutoConfig.class);

    @BeforeEach
    void attachAppender() {
        targetLogger = (Logger) LoggerFactory.getLogger(SLF4JQueryLoggingListener.class);
        targetLogger.setLevel(Level.DEBUG);

        appender = new ListAppender<>();
        appender.start();
        targetLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        targetLogger.detachAppender(appender);
        appender.stop();
    }

    @DisplayName("활성화만 하면 쿼리를 실행해도 SQL 로그가 남지 않는다")
    @Test
    void noSqlLogWhenLoggingDisabled() {
        runner.withPropertyValues("query-counter.enabled=true")
            .run(context -> {
                // given
                executeQuery(context.getBean(DataSource.class));

                // then
                assertThat(appender.list).isEmpty();
            });
    }

    @DisplayName("로깅을 켜면 실행한 쿼리가 SQL 로그로 남는다")
    @Test
    void sqlLogWhenLoggingEnabled() {
        runner.withPropertyValues("query-counter.enabled=true", "query-counter.logging.enabled=true")
            .run(context -> {
                // given
                executeQuery(context.getBean(DataSource.class));

                // then
                assertThat(appender.list).isNotEmpty();
                assertThat(appender.list)
                    .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("select 1"));
            });
    }

    private void executeQuery(DataSource dataSource) {
        new JdbcTemplate(dataSource).queryForObject("select 1", Integer.class);
    }

}
