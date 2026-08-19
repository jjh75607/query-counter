package soon.springtestutil.querycount.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다른 스레드에서 실제로 쿼리를 날려 합쳐지는지 봅니다.
 *
 * <p>톰캣 워커 스레드를 띄우지 않아도 성질은 같습니다. 테스트가 만들지 않은 스레드에서 나간
 * 쿼리라는 점이 같기 때문입니다.
 */
@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = {
    "query-counter.enabled=true",
    "query-counter.other-threads.enabled=true"
})
class OtherThreadIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS member");
        jdbcTemplate.execute("CREATE TABLE member (id INT PRIMARY KEY)");
        QueryCountContext.clear();
    }

    private void queryOnAnotherThread() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> done = executor.submit(
                () -> jdbcTemplate.queryForObject("SELECT count(*) FROM member", Integer.class));
            done.get();
        }
        finally {
            executor.shutdown();
        }
    }

    @DisplayName("다른 스레드에서 나간 쿼리가 합치기 전에는 안 보이고 합친 뒤에 보인다")
    @Test
    void otherThreadQueryShouldAppearAfterMerge() throws Exception {
        // given
        queryOnAnotherThread();

        // when, then - 합치기 전에는 이 스레드의 기록에 없다
        assertThat(QueryCountContext.getQueries()).isEmpty();

        QueryCountContext.mergeOtherThreadQueries();

        assertThat(QueryCountContext.getQueries())
            .extracting(QueryInfo::getQuery)
            .containsExactly("SELECT count(*) FROM member");
    }

    @DisplayName("테스트 스레드에서 나간 것과 다른 스레드에서 나간 것이 함께 세진다")
    @Test
    void bothThreadsShouldBeCountedTogether() throws Exception {
        // given
        jdbcTemplate.update("INSERT INTO member (id) VALUES (1)");
        queryOnAnotherThread();

        // when
        QueryCountContext.mergeOtherThreadQueries();

        // then
        assertThat(QueryCountContext.getQueryCounts())
            .containsEntry(soon.springtestutil.querycount.QueryType.INSERT, 1L)
            .containsEntry(soon.springtestutil.querycount.QueryType.SELECT, 1L);
    }

}
