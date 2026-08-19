package soon.springtestutil.querycount.assertion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.querycount.NPlusOneCheck;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 프로퍼티에서 테스트 경계까지 배선이 이어지는지 봅니다.
 *
 * <p>설정은 애플리케이션 컨텍스트에 있고 테스트를 끝내는 리스너는 그 컨텍스트를 만질 수
 * 없습니다. 그래서 모드가 DataSource 프록시를 타고 ThreadLocal 로 전달되는데, 그 경로가
 * 실제 쿼리 실행으로 이어지는지는 이 테스트만 볼 수 있습니다.
 */
@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = {
    "query-counter.enabled=true",
    "query-counter.n-plus-one.enabled=true",
    "query-counter.n-plus-one.fail=true"
})
class GlobalNPlusOneIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS team");
        jdbcTemplate.execute("CREATE TABLE team (id INT PRIMARY KEY, name VARCHAR(100))");
        jdbcTemplate.update("INSERT INTO team (id, name) VALUES (?, ?)", 1, "팀1");
        jdbcTemplate.update("INSERT INTO team (id, name) VALUES (?, ?)", 2, "팀2");
        QueryCountContext.clear();
    }

    @DisplayName("프로퍼티로 켠 모드가 실제 쿼리 실행을 통해 테스트 경계까지 전달된다")
    @Test
    void modeShouldReachTestBoundaryThroughRecordedQueries() {
        // given - 같은 SELECT 를 값만 바꿔 두 번 날린다. N+1 과 같은 모양이다
        jdbcTemplate.queryForObject("SELECT name FROM team WHERE id = ?", String.class, 1);
        jdbcTemplate.queryForObject("SELECT name FROM team WHERE id = ?", String.class, 2);

        // when
        NPlusOneCheck check = QueryCountContext.getNPlusOneCheck();

        // then - 기록하는 쪽이 모드를 실어 왔다
        assertThat(check).isEqualTo(NPlusOneCheck.FAIL);
        assertThatThrownBy(NPlusOneWatch::run)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("SELECT name FROM team WHERE id = ?");

        // 여기서 비우지 않으면 테스트가 끝날 때 리스너가 같은 N+1 을 찾아 이 테스트를
        // 실패시킨다. 실패시키는 것이 이 모드의 목적이므로 정상 동작이다.
        QueryCountContext.clear();
    }

    @DisplayName("쿼리를 하나도 안 날린 테스트에는 모드가 실려 오지 않는다")
    @Test
    void modeShouldStayOffWithoutQueries() {
        // given - initSchema 뒤에 아무 쿼리도 날리지 않는다

        // when, then
        assertThat(QueryCountContext.getNPlusOneCheck()).isEqualTo(NPlusOneCheck.OFF);
    }

}
