package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import soon.springtestutil.config.AutoConfig;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @ExtendWith} 없이 리스너만으로 동작하는지 확인한다.
 * 이 클래스에 {@code @ExtendWith(QueryCountTestExtension.class)} 를 일부러 붙이지 않았다.
 */
@DisplayName("ExtendWith 없이 리스너만으로 동작한다")
@EnableAutoConfiguration
@SpringBootTest(classes = AutoConfig.class, properties = "query-counter.enabled=true")
class QueryCountTestExecutionListenerTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DisplayName("ExtendWith 를 붙이지 않아도 테스트 이름이 기록된다")
    @Test
    void testContextIsRecordedWithoutExtendWith() {
        // given, when - 리스너가 beforeTestMethod 에서 기록했어야 한다

        // then
        assertThat(TestContextHolder.getContextInfo())
            .contains(getClass().getName())
            .contains("testContextIsRecordedWithoutExtendWith");
    }

    @DisplayName("ExtendWith 를 붙이지 않아도 실행한 쿼리가 기록되고 검증된다")
    @Test
    void queriesAreCountedWithoutExtendWith() {
        // given
        jdbcTemplate.queryForObject("select 1", Integer.class);

        // when, then
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

    @DisplayName("테스트가 시작될 때 기록된 쿼리가 비어 있다")
    @Test
    void recordedQueriesStartEmpty() {
        // given, when - 리스너가 beforeTestMethod 에서 정리했어야 한다

        // then
        assertThat(QueryCountContext.getQueries()).isEmpty();
    }

}
