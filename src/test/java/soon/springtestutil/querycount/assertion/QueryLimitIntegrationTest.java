package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.querycount.QueryLimit;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 프로퍼티에서 테스트 경계까지 상한이 전달되는지 봅니다.
 *
 * <p>설정은 애플리케이션 컨텍스트에 있고 테스트를 끝내는 리스너는 그 컨텍스트를 만질 수
 * 없으므로, 값이 DataSource 프록시를 타고 ThreadLocal 로 옵니다. 그 경로가 실제 쿼리 실행으로
 * 이어지는지는 이 테스트만 봅니다.
 */
@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = {
    "query-counter.enabled=true",
    "query-counter.max-queries.per-test=2"
})
class QueryLimitIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS member");
        jdbcTemplate.execute("CREATE TABLE member (id INT PRIMARY KEY)");
        QueryCountContext.clear();
    }

    @DisplayName("프로퍼티로 준 상한이 실제 쿼리 실행을 통해 테스트 경계까지 전달된다")
    @Test
    void limitShouldReachTestBoundary() {
        // given
        jdbcTemplate.update("INSERT INTO member (id) VALUES (1)");

        // when
        QueryLimit limit = QueryCountContext.getQueryLimit();

        // then
        assertThat(limit).isEqualTo(QueryLimit.of(2, false));

        // 상한을 넘겨 놓고 판정을 부른다. 여기서 비우지 않으면 테스트가 끝날 때 리스너가
        // 같은 초과를 찾아 이 테스트를 실패시킨다. 실패시키는 것이 이 기능의 목적이다.
        jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class);
        jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class);
        assertThatThrownBy(QueryLimitWatch::run)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("the limit is 2");

        QueryCountContext.clear();
    }

}
