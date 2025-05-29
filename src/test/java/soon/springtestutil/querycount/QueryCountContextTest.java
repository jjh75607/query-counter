package soon.springtestutil.querycount;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("쿼리 유형을 증가시키면 해당 유형의 카운트가 증가한다")
    void incrementOnceShouldUpdateCount() {
        // given
        QueryType queryType = QueryType.SELECT;

        // when
        QueryCountContext.increment(queryType);
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();

        // then
        assertThat(counts).containsEntry(queryType, 1L);
    }

    @DisplayName("동일한 쿼리 유형을 여러 번 증가시키면 해당 유형의 카운트만 누적된다.")
    @Test
    void incrementMultipleTimesShouldUpdateCountCorrectly() {
        // given
        QueryType queryType = QueryType.SELECT;

        // when
        for (int i = 0; i < 5; i++) {
            QueryCountContext.increment(queryType);
        }

        // then
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();
        assertThat(counts).containsEntry(queryType, 5L);
    }

    @DisplayName("다른 쿼리 유형을 증가시키면 해당 유형의 카운트가 별도로 유지된다.")
    @Test
    void incrementDifferentQueryTypesShouldMaintainSeparateCounts() {
        // given
        QueryType selectType = QueryType.SELECT;
        QueryType insertType = QueryType.INSERT;

        // when
        QueryCountContext.increment(selectType);
        QueryCountContext.increment(insertType);
        Map<QueryType, Long> counts = QueryCountContext.getQueryCounts();

        // then
        assertThat(counts).containsEntry(selectType, 1L);
        assertThat(counts).containsEntry(insertType, 1L);
    }

    @DisplayName("클리어 호출 시 쿼리 카운트가 초기화 된다.")
    @Test
    void clearShouldResetQueryCounts() {
        // given
        QueryType selectType = QueryType.SELECT;
        QueryType insertType = QueryType.INSERT;

        QueryCountContext.increment(selectType);
        QueryCountContext.increment(insertType);

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
                QueryCountContext.increment(selectType);
                QueryCountContext.increment(selectType);
                assertThat(QueryCountContext.getQueryCounts())
                    .isEqualTo(Map.of(selectType, 2L));
            } finally {
                QueryCountContext.clear();
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                QueryCountContext.increment(insertType);
                QueryCountContext.increment(updateType);
                QueryCountContext.increment(insertType);
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