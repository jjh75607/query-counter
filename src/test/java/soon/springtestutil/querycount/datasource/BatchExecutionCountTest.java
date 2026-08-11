package soon.springtestutil.querycount.datasource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import javax.sql.DataSource;
import java.util.List;

/**
 * JDBC 배치로 보낸 문장이 몇 건으로 집계되는지 고정한다.
 *
 * <p>몇 건을 쌓았든 1건이다. datasource-proxy 가 배치를 목록 원소 하나에 파라미터 묶음
 * 여러 개로 넘기고, {@code QueryCountListener} 가 그 목록을 순회하기 때문이다. JDBC 왕복
 * 횟수로 세면 1건이 맞다.
 *
 * <p>버그가 아니라 현재 동작이지만 사용자의 기대가 갈리는 지점이다. Hibernate 의
 * {@code hibernate.jdbc.batch_size} 를 켠 프로젝트에서 엔티티 10건을 저장해도 카운트는
 * 1이므로, 라이브러리가 쿼리를 놓쳤다고 오해하기 쉽다. README 에 이 기준을 밝혀두었고
 * 이 테스트가 그 문서와 코드를 함께 묶는다.
 *
 * <p>집계 기준을 바꾸려면 이 테스트가 먼저 깨져야 한다. 조용히 바뀌면 기존 사용자의 테스트가
 * 예고 없이 깨진다.
 */
@DisplayName("JDBC 배치로 보낸 문장은")
@SpringBootTest(
    classes = {AutoConfig.class, BatchExecutionCountTest.BatchTestConfig.class},
    properties = "query-counter.enabled=true"
)
class BatchExecutionCountTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class BatchTestConfig {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
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

    @DisplayName("몇 건을 쌓았든 1건으로 집계된다")
    @Test
    void batchedInsertsAreCountedAsOne() {
        // given
        jdbcTemplate.batchUpdate(
            "INSERT INTO member (id, name) VALUES (?, ?)",
            List.of(new Object[] {1, "a"}, new Object[] {2, "b"}, new Object[] {3, "c"})
        );

        // expected
        QueryCounterAssertion.assertCounts()
            .insert(1)
            .verify();
    }

    @DisplayName("같은 문장을 배치 없이 보내면 보낸 횟수만큼 집계된다")
    @Test
    void separateInsertsAreCountedIndividually() {
        // given
        jdbcTemplate.update("INSERT INTO member (id, name) VALUES (?, ?)", 1, "a");
        jdbcTemplate.update("INSERT INTO member (id, name) VALUES (?, ?)", 2, "b");
        jdbcTemplate.update("INSERT INTO member (id, name) VALUES (?, ?)", 3, "c");

        // expected
        QueryCounterAssertion.assertCounts()
            .insert(3)
            .verify();
    }

}
