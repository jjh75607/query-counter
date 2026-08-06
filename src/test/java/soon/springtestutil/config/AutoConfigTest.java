package soon.springtestutil.config;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("자동 설정은 활성화하지 않으면 아무것도 하지 않는다")
class AutoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
        .withUserConfiguration(AutoConfig.class);

    @DisplayName("프로퍼티를 지정하지 않으면 자동 설정이 적용되지 않고 DataSource도 감싸지지 않는다")
    @Test
    void notEnabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(DataSourceProxyBeanPostProcessor.class);
            assertThat(context).doesNotHaveBean(QueryCounterProperties.class);
            assertThat(context.getBean(DataSource.class)).isNotInstanceOf(ProxyDataSource.class);
        });
    }

    @DisplayName("enabled=false 를 명시해도 자동 설정이 적용되지 않는다")
    @Test
    void notEnabledWhenExplicitlyFalse() {
        runner.withPropertyValues("query-counter.enabled=false")
            .run(context -> assertThat(context)
                .doesNotHaveBean(DataSourceProxyBeanPostProcessor.class));
    }

    @DisplayName("enabled=true 이면 BeanPostProcessor 가 등록된다")
    @Test
    void enabledWhenPropertyIsTrue() {
        runner.withPropertyValues("query-counter.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(DataSourceProxyBeanPostProcessor.class);
                assertThat(context).hasSingleBean(QueryCounterProperties.class);
            });
    }

    @DisplayName("SQL 로깅 기본값은 비활성이고 프로퍼티로 켤 수 있다")
    @Test
    void loggingIsDisabledByDefault() {
        runner.withPropertyValues("query-counter.enabled=true")
            .run(context -> assertThat(context.getBean(QueryCounterProperties.class)
                .getLogging()
                .isEnabled()).isFalse());

        runner.withPropertyValues("query-counter.enabled=true", "query-counter.logging.enabled=true")
            .run(context -> assertThat(context.getBean(QueryCounterProperties.class)
                .getLogging()
                .isEnabled()).isTrue());
    }

}
