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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = "query-counter.enabled=true")
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

    /**
     * quick-perf 에는 실행 시간 경고 메시지가 설정한 값 대신 항상 {@code 0 ms} 로 찍히던
     * 결함이 있었다(quick-perf/quickperf#93). 같은 datasource-proxy 위에서 도는
     * 라이브러리라 확인이 필요했고, 여기서는 설정값과 실측값이 모두 메시지에 들어간다.
     * 이 테스트는 그 성질을 지킨다.
     */
    @Test
    @DisplayName("실행 시간 위반 메시지에 설정한 상한과 실제 측정값이 함께 들어간다")
    void executionTimeMessageCarriesConfiguredMaxAndMeasuredTime() {
        // given
        jdbcTemplate.execute("CALL SLEEP(120)");

        // when
        AssertionError error = catchThrowableOfType(
            () -> QueryCounterAssertion.assertCounts()
                .maxExecutionTimeMs(50)
                .verify(),
            AssertionError.class
        );

        // then
        assertThat(error).hasMessageContaining("max=50ms");
        assertThat(error.getMessage())
            .as("실측값이 0 으로 찍히지 않고 상한을 넘은 실제 시간이 들어가야 한다")
            .containsPattern("\\[1] (\\d+)ms > 50ms");
        assertThat(measuredMsIn(error.getMessage())).isGreaterThan(50L);
    }

    private long measuredMsIn(String message) {
        Matcher matcher = Pattern.compile("\\[1] (\\d+)ms > 50ms").matcher(message);
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
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
