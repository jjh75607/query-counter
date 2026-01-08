package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.querycount.context.QueryCountContext;
import soon.springtestutil.querycount.extension.QueryCountTestExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class)
@ExtendWith(QueryCountTestExtension.class)
public class QueryCountAssertionIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS orders");
        jdbcTemplate.execute("DROP TABLE IF EXISTS member");
        jdbcTemplate.execute("CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(100))");
        jdbcTemplate.execute("CREATE TABLE orders (id INT PRIMARY KEY, member_id INT, amount INT)");
        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS SLEEP FOR \"java.lang.Thread.sleep\"");
        QueryCountContext.clear();
    }

    @Test
    @DisplayName("실제 DB 쿼리 실행 시 maxExecutionTime(ms)를 초과하면 실패한다")
    void verifyMaxExecutionTimeShouldFailWhenExceeded() {
        // given
        jdbcTemplate.execute("CALL SLEEP(120)");

        // expected
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .maxExecutionTimeMs(50)
            .verify()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Query execution time assertion failed")
            .hasMessageContaining("violations=");
    }

    @Test
    @DisplayName("maxExecutionTimeMs 미사용 시 카운트 검증만 수행된다")
    void countOnlyUnaffectedWithoutMaxTime() {
        // given
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

}
