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
 * }</pre>
 */
@ConfigurationProperties(prefix = "query-counter")
public class QueryCounterProperties {

    /**
     * Whether to count queries. When enabled, the DataSource is wrapped in a proxy that records every executed query.
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
