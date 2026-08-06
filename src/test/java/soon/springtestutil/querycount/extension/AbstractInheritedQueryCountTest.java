package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

/**
 * 테스트 메서드를 상위 클래스에 두고 하위 클래스가 상속받는 구조에서도 기록이 도는지
 * 확인하기 위한 상위 클래스다. 실행되는 것은 하위 클래스인
 * {@link InheritedQueryCountTest} 이다.
 *
 * <p>quick-perf 에는 테스트 클래스 계층에서 동작하지 않던 결함이 보고된 적이 있다
 * (quick-perf/quickperf#191). 같은 datasource-proxy 위에서 도는 라이브러리라 확인이
 * 필요했고, 결함은 없었다. 이 테스트는 그 성질을 지킨다.
 */
@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = "query-counter.enabled=true")
abstract class AbstractInheritedQueryCountTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS member");
        jdbcTemplate.execute("CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(100))");
        QueryCountContext.clear();
    }

    @DisplayName("상위 클래스에 선언된 테스트 메서드에서도 쿼리가 기록된다")
    @Test
    void queriesAreRecordedInInheritedTestMethod() {
        // given
        jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class);

        // expected
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

}
