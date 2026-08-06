package soon.springtestutil.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import javax.sql.DataSource;

/**
 * DataSource 빈이 다른 DataSource 빈을 위임할 때 쿼리가 한 번만 기록되는지 확인한다.
 *
 * <p>이 구성에서는 빈이 이렇게 만들어진다. 안쪽 DataSource가 먼저 만들어져 감싸지고,
 * 바깥 DataSource가 감싸진 안쪽 것을 위임 대상으로 주입받는다. 바깥 것까지 감싸면 쿼리가
 * 바깥에서 한 번, 위임된 안쪽에서 한 번, 두 번 기록된다.
 *
 * <p>{@code LazyConnectionDataSourceProxy}로 DataSource를 감싸는 구성은 Spring
 * 애플리케이션에서 흔하다. 실제 트랜잭션이 시작될 때까지 커넥션 획득을 미루려고 쓴다.
 *
 * <p>깨지면 사용자의 모든 카운트가 조용히 두 배가 된다. 자기 코드가 쿼리를 두 번 날린다고
 * 오해하게 되므로 실패 방식이 나쁘다.
 */
@DisplayName("DataSource 빈이 다른 DataSource 빈을 위임할 때")
@SpringBootTest(
    classes = {AutoConfig.class, DelegatingDataSourceTest.DelegatingDataSourceConfig.class},
    properties = "query-counter.enabled=true"
)
class DelegatingDataSourceTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class DelegatingDataSourceConfig {

        @Bean
        DataSource realDataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        }

        @Bean
        @Primary
        DataSource lazyDataSource(DataSource realDataSource) {
            return new LazyConnectionDataSourceProxy(realDataSource);
        }

        /**
         * JdbcTemplate 을 직접 등록한다. Spring Boot 의 자동 설정을 쓰면
         * {@code DataSourceAutoConfiguration} 을 제외해야 하는데, 그 클래스는 Spring Boot 4
         * 에서 패키지가 옮겨져 버전에 묶인다.
         */
        @Bean
        JdbcTemplate jdbcTemplate(@Qualifier("lazyDataSource") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
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

    @DisplayName("쿼리 한 번 실행이 한 건으로 기록된다")
    @Test
    void queryIsRecordedOnce() {
        // given
        jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

    @DisplayName("INSERT 도 한 건으로 기록된다")
    @Test
    void insertIsRecordedOnce() {
        // given
        jdbcTemplate.update("INSERT INTO member (id, name) VALUES (1, 'a')");

        // expected
        QueryCounterAssertion.assertCounts()
            .insert(1)
            .verify();
    }

}
