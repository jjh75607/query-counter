package soon.springtestutil.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;

class TestContextHolderTest {

    @AfterEach
    void tearDown() {
        TestContextHolder.clearContext();
    }

    @DisplayName("테스트 컨텍스트 정보를 조회한다.")
    @Test
    void contextInfoSelected() {
        // given
        String className = "com.example.TestContextHolderTest";
        String methodName = "contextInfoSelected";
        String expected = "[Test: %s#%s] ".formatted(className, methodName);

        // when
        TestContextHolder.setContext(className, methodName);

        // then
        String contextInfo = TestContextHolder.getContextInfo();
        assertThat(contextInfo)
            .isEqualTo(expected);
    }

    @DisplayName("컨텍스트 정보가 설정되지 않은 경우 빈 문자열이 반환된다.")
    @Test
    void contextInfoReturnsEmptyWhenNotSet() {
        // given
        String contextInfo = TestContextHolder.getContextInfo();

        // expected
        assertThat(contextInfo)
            .isEmpty();
    }

    @DisplayName("여러 스레드에서 컨텍스트 정보는 격리된다.")
    @Test
    void contextInfoIsThreadSpecific() throws InterruptedException {
        // given
        ExecutorService executorService = newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        String thread1ClassName = "com.example.Thread1Test";
        String thread1MethodName = "thread1Method";
        String thread2ClassName = "com.example.Thread2Test";
        String thread2MethodName = "thread2Method";

        // excepted
        executorService.submit(() -> {
            try {
                TestContextHolder.setContext(thread1ClassName, thread1MethodName);
                String contextInfo = TestContextHolder.getContextInfo();

                assertThat(contextInfo)
                    .isEqualTo("[Test: %s#%s] ".formatted(thread1ClassName, thread1MethodName));
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                TestContextHolder.setContext(thread2ClassName, thread2MethodName);
                String contextInfo = TestContextHolder.getContextInfo();

                assertThat(contextInfo)
                    .isEqualTo("[Test: %s#%s] ".formatted(thread2ClassName, thread2MethodName));
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        assertThat(TestContextHolder.getContextInfo())
            .isEmpty();
    }

}