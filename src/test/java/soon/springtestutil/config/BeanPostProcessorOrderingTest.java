package soon.springtestutil.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import javax.sql.DataSource;

/**
 * DataSource에 의존하는 다른 BeanPostProcessor가 있어도 DataSource가 감싸지는지 확인한다.
 *
 * <p>Spring은 BeanPostProcessor를 {@code PriorityOrdered}, {@code Ordered}, 나머지 순으로
 * 만든다. 여기 있는 것처럼 {@code Ordered}를 구현하면서 DataSource를 주입받는
 * BeanPostProcessor가 있으면 그것을 만드는 과정에서 DataSource가 함께 만들어진다.
 * 그 시점에 이 라이브러리의 BeanPostProcessor가 아직 등록되지 않았다면 DataSource는
 * 감싸지지 않고 기록되는 쿼리가 0건이 된다.
 *
 * <p>깨지면 카운트가 어긋나는 것이 아니라 아무것도 기록되지 않는다. 그때 붙는 안내가
 * {@code query-counter.enabled=true}를 설정했는지 묻는 것이라 원인과 어긋나서 더 헤매게
 * 된다. 그래서 이 성질은 테스트로 지킨다.
 *
 * <p>quick-perf도 같은 문제를 겪고 2026년 4월에 고쳤다(quick-perf/quickperf#260).
 */
@DisplayName("DataSource에 의존하는 다른 BeanPostProcessor 가 있을 때")
@EnableAutoConfiguration
@SpringBootTest(
    classes = {AutoConfig.class, BeanPostProcessorOrderingTest.OrderedBeanPostProcessorConfig.class},
    properties = "query-counter.enabled=true"
)
class BeanPostProcessorOrderingTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class OrderedBeanPostProcessorConfig {

        @Bean
        static BeanPostProcessor beanPostProcessorDependingOnDataSource(DataSource dataSource) {
            return new DataSourceDependentBeanPostProcessor(dataSource);
        }

    }

    /**
     * DataSource를 주입받으면서 {@code Ordered}를 구현합니다. 사용자 프로젝트에서 흔한 형태입니다.
     */
    record DataSourceDependentBeanPostProcessor(DataSource dataSource)
        implements BeanPostProcessor, Ordered {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS member");
        jdbcTemplate.execute("CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(100))");
        QueryCountContext.clear();
    }

    @DisplayName("DataSource 가 그대로 감싸져 쿼리가 기록된다")
    @Test
    void queriesAreStillRecorded() {
        // given
        jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

}
