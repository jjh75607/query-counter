package soon.springtestutil.querycount;

/**
 * What happens when the N+1 check runs for every test and finds something.
 *
 * <p>Turned on with {@code query-counter.n-plus-one.enabled=true}. It is {@link #OFF}
 * unless that is set, so adding this library cannot change how existing tests behave.
 *
 * <p>{@link #REPORT} is the default once it is on. Turning the check on in a project that
 * has never had it will surface every N+1 already in the suite at once, and failing all of
 * them on the first run leaves no way forward other than switching the check back off.
 * Report first, work through the list, then move to {@link #FAIL}.
 */
public enum NPlusOneCheck {

    /** No check runs. */
    OFF,

    /** A warning is logged. Tests still pass. */
    REPORT,

    /** The test fails with an {@code AssertionError}. */
    FAIL;

    /**
     * Resolves the two properties into a mode.
     *
     * @param enabled {@code query-counter.n-plus-one.enabled}
     * @param fail {@code query-counter.n-plus-one.fail}
     */
    public static NPlusOneCheck of(boolean enabled, boolean fail) {
        if (!enabled) {
            return OFF;
        }
        return fail ? FAIL : REPORT;
    }

}
