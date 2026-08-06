package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

/**
 * 파라미터화 테스트의 반복마다 기록이 초기화되는지 확인한다.
 *
 * <p>초기화가 반복 단위가 아니라 메서드 단위로 일어나면 두 번째 반복부터 앞선 반복의 쿼리가
 * 누적되어 카운트가 계속 늘어난다. quick-perf 에는 파라미터화 테스트를 지원하지 못하던
 * 시기가 있었고(quick-perf/quickperf#139) 같은 datasource-proxy 위에서 도는 라이브러리라
 * 확인이 필요했다. 확인 결과 반복마다 정상적으로 초기화되며, 이 테스트는 그 성질을 지킨다.
 */
@DisplayName("파라미터화 테스트의 반복 간 격리")
@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = "query-counter.enabled=true")
class ParameterizedTestIsolationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS member");
        jdbcTemplate.execute("CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(100))");
        QueryCountContext.clear();
    }

    @DisplayName("반복마다 기록이 초기화되어 앞선 반복의 쿼리가 섞이지 않는다")
    @ParameterizedTest(name = "{0}번째 반복")
    @ValueSource(ints = {1, 2, 3})
    void recordedQueriesAreResetForEachInvocation(int invocation) {
        // given
        jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

}
