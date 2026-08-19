package consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 발행물을 의존성으로 받아 실제로 호출한다.
 *
 * <p>프로젝트 소스가 아니라 Maven 저장소에서 받은 jar 를 쓴다. 그래서 의존성 해석,
 * 자동 설정 파일과 리스너 등록 파일이 jar 에 실려 나갔는지, 그리고 사용자 쪽에서 실제로
 * 호출되는지까지 한 번에 드러난다. {@code 0.2.0} 은 이 검증이 릴리스 뒤에만 돌던 틈으로
 * 나갔다.
 */
@DisplayName("발행된 query-counter 를 소비자로서 쓴다")
@SpringBootTest(classes = SmokeApp.class, properties = "query-counter.enabled=true")
class PublishedLibrarySmokeTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DisplayName("실행한 쿼리 종류별 횟수를 센다")
    @Test
    void countsQueriesByType() {
        // given
        createTable();
        QueryCountContext.clear();
        jdbcTemplate.update("insert into member (id) values (1)");

        // when
        jdbcTemplate.queryForObject("select count(*) from member", Integer.class);

        // then
        QueryCounterAssertion.assertCounts()
            .insert(1)
            .select(1)
            .verify();
    }

    @DisplayName("기대와 실제가 다르면 실패시킨다")
    @Test
    void failsWhenCountsDoNotMatch() {
        // given
        createTable();
        QueryCountContext.clear();

        // when
        jdbcTemplate.queryForObject("select count(*) from member", Integer.class);

        // then - 세기만 하고 판정을 안 하는 상태에서도 위 테스트는 통과한다.
        // 실패가 실제로 실패로 나오는지는 이쪽이 지킨다.
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts().select(99).verify())
            .isInstanceOf(AssertionError.class);
    }

    private void createTable() {
        jdbcTemplate.execute("create table if not exists member (id int primary key)");
        jdbcTemplate.update("delete from member");
    }

}
