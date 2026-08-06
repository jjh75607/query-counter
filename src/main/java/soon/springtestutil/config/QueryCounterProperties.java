package soon.springtestutil.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * query-counter 동작을 제어하는 설정입니다.
 *
 * <p>기본값은 모두 비활성입니다. 활성화하지 않으면 DataSource를 감싸지 않으므로
 * 이 라이브러리를 쓰지 않는 테스트에는 아무 영향을 주지 않습니다.
 *
 * <pre>{@code
 * query-counter:
 *   enabled: true
 *   logging:
 *     enabled: false
 * }</pre>
 */
@ConfigurationProperties(prefix = "query-counter")
public class QueryCounterProperties {

    /**
     * 쿼리 카운팅 활성화 여부입니다. 켜면 DataSource를 프록시로 감싸 실행된 쿼리를 기록합니다.
     */
    private boolean enabled = false;

    private final Logging logging = new Logging();

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Logging getLogging() {
        return this.logging;
    }

    /**
     * 실행된 SQL을 로그로 출력하는 설정입니다. 쿼리 카운팅과는 별개이며 기본값은 비활성입니다.
     */
    public static class Logging {

        /**
         * 실행된 SQL을 SLF4J로 출력할지 여부입니다.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
