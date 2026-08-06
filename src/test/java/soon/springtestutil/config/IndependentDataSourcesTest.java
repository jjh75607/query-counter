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
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import javax.sql.DataSource;

/**
 * 서로 위임하지 않는 DataSource 빈이 둘일 때 각각의 쿼리가 모두 기록되는지 확인한다.
 *
 * <p>위임 구조에서 중복 기록을 막는 판정이 너무 넓어지면 이런 구성에서 두 번째 DataSource의
 * 쿼리를 놓치게 된다. 놓치는 실패도 조용하다. 그래서 반대 방향을 함께 지킨다.
 *
 * <p>DataSource를 둘 두는 구성은 읽기와 쓰기를 나누거나 서로 다른 데이터베이스를 함께 쓸 때
 * 나온다.
 */
@DisplayName("서로 위임하지 않는 DataSource 빈이 둘일 때")
@SpringBootTest(
    classes = {AutoConfig.class, IndependentDataSourcesTest.TwoDataSourcesConfig.class},
    properties = "query-counter.enabled=true"
)
class IndependentDataSourcesTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TwoDataSourcesConfig {

        @Bean
        @Primary
        DataSource firstDataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        }

        @Bean
        DataSource secondDataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        }

        /**
         * JdbcTemplate 을 직접 등록한다. Spring Boot 의 자동 설정을 쓰면
         * {@code DataSourceAutoConfiguration} 을 제외해야 하는데, 그 클래스는 Spring Boot 4
         * 에서 패키지가 옮겨져 버전에 묶인다.
         */
        @Bean
        @Primary
        JdbcTemplate firstJdbcTemplate(@Qualifier("firstDataSource") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        JdbcTemplate secondJdbcTemplate(@Qualifier("secondDataSource") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

    }

    @Autowired
    @Qualifier("firstJdbcTemplate")
    JdbcTemplate first;

    @Autowired
    @Qualifier("secondJdbcTemplate")
    JdbcTemplate second;

    @BeforeEach
    void initSchema() {
        first.execute("DROP TABLE IF EXISTS member");
        first.execute("CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(100))");
        second.execute("DROP TABLE IF EXISTS member");
        second.execute("CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(100))");
        QueryCountContext.clear();
    }

    @DisplayName("양쪽 DataSource 의 쿼리가 모두 기록된다")
    @Test
    void queriesFromBothDataSourcesAreRecorded() {
        // given
        first.queryForObject("SELECT count(*) FROM member", Integer.class);
        second.queryForObject("SELECT count(*) FROM member", Integer.class);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(2)
            .verify();
    }

}
