package soon.springtestutil.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings that control query-counter.
 *
 * <p>Everything is disabled by default. When not enabled, the DataSource is left
 * untouched, so this library has no effect on tests that do not use it.
 *
 * <pre>{@code
 * query-counter:
 *   enabled: true
 *   logging:
 *     enabled: false
 *   n-plus-one:
 *     enabled: false
 *     fail: false
 * }</pre>
 */
@ConfigurationProperties(prefix = "query-counter")
public class QueryCounterProperties {

    /**
     * Whether to count queries. When enabled, the DataSource is wrapped in a proxy that records every executed query.
     */
    private boolean enabled = false;

    private final Logging logging = new Logging();

    private final NPlusOne nPlusOne = new NPlusOne();

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Logging getLogging() {
        return this.logging;
    }

    public NPlusOne getNPlusOne() {
        return this.nPlusOne;
    }

    /**
     * The N+1 check that runs for every test without an assertion being written.
     *
     * <p>Disabled by default. Turning it on in a suite that has never had it will surface
     * every N+1 already there at once, so it only warns until {@code fail} is set as well.
     */
    public static class NPlusOne {

        /**
         * Whether to check every test for an N+1, with no assertion written in the test.
         */
        private boolean enabled = false;

        /**
         * Whether a detected N+1 fails the test. When false, it is logged as a warning.
         */
        private boolean fail = false;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isFail() {
            return this.fail;
        }

        public void setFail(boolean fail) {
            this.fail = fail;
        }

    }

    /**
     * Logging of executed SQL. Independent of query counting and disabled by default,
     * because always-on SQL logging is noisy in projects with many tests.
     */
    public static class Logging {

        /**
         * Whether to log every executed SQL statement through SLF4J.
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
