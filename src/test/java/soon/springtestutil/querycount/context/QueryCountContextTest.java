package soon.springtestutil.querycount.context;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.querycount.QueryType;

class QueryCountContextTest {

    @AfterEach
    void tearDown() {
        QueryCountContext.clear();
    }

    @Test
    @DisplayName("초기 쿼리 카운트는 비어 있다.")
    void initialQueryCountsShouldBeEmpty() {
        // given
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();

        // expected
        assertThat(counts).isEmpty();
    }

    @Test
    @DisplayName("쿼리 정보를 추가하면 해당 유형의 카운트가 증가한다")
    void addQueryOnceShouldUpdateCount() {
        // given
        QueryType queryType = QueryType.SELECT;
        String query = "SELECT * FROM member";

        // when
        QueryCountContext.addQuery(queryType, query);
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();

        // then
        assertThat(counts).containsEntry(queryType, 1L);
    }

    @DisplayName("동일한 쿼리 유형을 여러 번 추가하면 해당 유형의 카운트만 누적된다.")
    @Test
    void addQueryMultipleTimesShouldUpdateCountCorrectly() {
        // given
        QueryType queryType = QueryType.SELECT;
        String query = "SELECT * FROM member";

        // when
        for (int i = 0; i < 5; i++) {
            QueryCountContext.addQuery(queryType, query);
        }

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(queryType, 5L);
    }

    @DisplayName("다른 쿼리 유형을 추가하면 해당 유형의 카운트가 별도로 유지된다.")
    @Test
    void addQueryDifferentQueryTypesShouldMaintainSeparateCounts() {
        // given
        QueryType selectType = QueryType.SELECT;
        String selectQuery = "SELECT * FROM member";
        QueryType insertType = QueryType.INSERT;
        String insertQuery = "INSERT INTO member (id, name) VALUES (1, 'test')";

        // when
        QueryCountContext.addQuery(selectType, selectQuery);
        QueryCountContext.addQuery(insertType, insertQuery);

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(selectType, 1L);
        assertThat(counts).containsEntry(insertType, 1L);
    }

    @DisplayName("쿼리 정보를 초기화하면 모든 카운트가 0이 된다.")
    @Test
    void clearShouldResetAllQueries() {
        // given
        QueryCountContext.addQuery(QueryType.SELECT, "SELECT * FROM member");
        QueryCountContext.addQuery(QueryType.INSERT, "INSERT INTO member (id, name) VALUES (1, 'test')");

        // when
        QueryCountContext.clear();

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).isEmpty();
    }

    @Test
    @DisplayName("쿼리 카운트를 가져올 때는 복사본이 반환된다.")
    void getQueryCountsShouldReturnCopy() {
        // given
        EnumMap<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        EnumMap<QueryType, Long> counts2 = QueryCountContext.getQueryCounts();

        // expected
        assertThat(counts).isNotSameAs(counts2);
    }

    @Test
    @DisplayName("여러 스레드에서 쿼리 카운트는 격리된다.")
    void queryCountsShouldBeIsolatedBetweenThreads() throws InterruptedException {
        // given
        QueryType selectType = QueryType.SELECT;
        QueryType insertType = QueryType.INSERT;
        QueryType updateType = QueryType.UPDATE;

        int numberOfThreads = 3;
        ExecutorService executorService = newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // when
        executorService.submit(() -> {
            try {
                QueryCountContext.addQuery(selectType, "SELECT 1");
                QueryCountContext.addQuery(selectType, "SELECT 2");
                assertThat(QueryCountContext.getQueryCounts())
                    .isEqualTo(Map.of(selectType, 2L));
            } finally {
                QueryCountContext.clear();
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                QueryCountContext.addQuery(insertType, "INSERT 1");
                QueryCountContext.addQuery(updateType, "UPDATE 1");
                QueryCountContext.addQuery(insertType, "INSERT 2");
                assertThat(QueryCountContext.getQueryCounts())
                    .isEqualTo(Map.of(insertType, 2L, updateType, 1L));
            } finally {
                QueryCountContext.clear();
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                assertThat(QueryCountContext.getQueryCounts()).isEmpty();
            } finally {
                QueryCountContext.clear();
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        // then
        assertThat(QueryCountContext.getQueryCounts()).isEmpty();
    }

}