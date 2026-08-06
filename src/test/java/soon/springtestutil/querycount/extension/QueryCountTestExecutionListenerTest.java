package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    /**
     * 아래 두 테스트는 순서가 의미를 가진다. 앞 테스트가 쿼리를 실행하고 어서션을 만들지 않아
     * 스스로 정리하지 않으므로, 다음 테스트가 비어 있다면 그것은 리스너가 정리한 것이다.
     * 순서를 고정하지 않으면 뒤 테스트가 먼저 돌아 헛되게 통과할 수 있다.
     */
    @DisplayName("어서션 없이 쿼리만 실행해 정리 대상을 남긴다")
    @Order(1)
    @Test
    void leavesRecordedQueryBehind() {
        // given, when
        jdbcTemplate.queryForObject("select 1", Integer.class);

        // then - 기록은 됐고 아무도 정리하지 않았다
        assertThat(QueryCountContext.getQueries()).isNotEmpty();
    }

    @DisplayName("앞 테스트가 남긴 쿼리를 리스너가 정리해 다음 테스트로 넘어오지 않는다")
    @Order(2)
    @Test
    void queriesAreIsolatedBetweenTestsWithoutExtendWith() {
        // given - 앞 테스트가 쿼리를 남겼고 어서션은 만들지 않았다

        // then - 리스너의 afterTestMethod 만이 이것을 비울 수 있다
        assertThat(QueryCountContext.getQueries()).isEmpty();
    }

}
