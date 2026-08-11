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

/**
 * H2 의 데이터 변경 델타 테이블 문장이 안쪽 문장의 타입으로 집계되는지 확인한다.
 *
 * <p>H2 는 자동 증가 키를 돌려줄 때 {@code select ID from final table (insert into ...)}
 * 형태를 보낸다. 맨 앞 키워드로 판정하면 SELECT 가 되어 {@code insert(1)} 을 기대한 테스트가
 * 0건으로 실패한다.
 *
 * <p>왕복은 1회이고 사용자가 의도한 동작은 저장이므로 안쪽 문장의 타입으로 1건을 센다.
 * 배치 집계와 같은 기준이다.
 */
@DisplayName("H2 의 데이터 변경 델타 테이블 문장은")
@SpringBootTest(
    classes = {AutoConfig.class, DataChangeDeltaTableCountTest.DeltaTableTestConfig.class},
    properties = "query-counter.enabled=true"
)
class DataChangeDeltaTableCountTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class DeltaTableTestConfig {

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
        jdbcTemplate.execute("CREATE TABLE member (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100))");
        QueryCountContext.clear();
    }

    @DisplayName("INSERT 1건으로 집계된다")
    @Test
    void finalTableInsertIsCountedAsInsert() {
        // given
        jdbcTemplate.queryForObject(
            "select ID from final table (insert into member (name) values ('a'))",
            Integer.class
        );

        // expected
        QueryCounterAssertion.assertCounts()
            .insert(1)
            .select(0)
            .verify();
    }

    @DisplayName("델타 키워드가 아니라 대상 테이블로 좁혀진다")
    @Test
    void finalTableInsertIsAttributedToTargetTable() {
        // given
        jdbcTemplate.queryForObject(
            "select ID from final table (insert into member (name) values ('a'))",
            Integer.class
        );

        // expected
        QueryCounterAssertion.assertCounts()
            .forTables("member")
            .insert(1)
            .verify();
    }

    @DisplayName("UPDATE 를 감싸면 UPDATE 1건으로 집계된다")
    @Test
    void oldTableUpdateIsCountedAsUpdate() {
        // given
        jdbcTemplate.update("insert into member (name) values ('a')");
        QueryCountContext.clear();
        jdbcTemplate.queryForObject(
            "select ID from old table (update member set name = 'b' where name = 'a')",
            Integer.class
        );

        // expected
        QueryCounterAssertion.assertCounts()
            .update(1)
            .select(0)
            .verify();
    }

}
