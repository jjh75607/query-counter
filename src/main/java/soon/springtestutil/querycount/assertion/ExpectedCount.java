package soon.springtestutil.querycount.assertion;

/**
 * An expected query count together with how it is compared against the actual count.
 *
 * <p>A plain number means an exact match, so the two calls below are the same.
 *
 * <pre>{@code
 * .select(3)
 * .select(exactly(3))
 * }</pre>
 *
 * <p>Use {@link #atMost(long)} when an upper bound is what matters. Tests that should keep
 * passing after a small implementation change are usually of this kind.
 *
 * <pre>{@code
 * import static soon.springtestutil.querycount.assertion.ExpectedCount.atMost;
 *
 * QueryCounterAssertion.assertCounts()
 *     .select(atMost(3))
 *     .verify();
 * }</pre>
 */
public final class ExpectedCount {

    private final long count;
    private final boolean upperBound;

    private ExpectedCount(long count, boolean upperBound) {
        if (count < 0) {
            throw new IllegalArgumentException("Expected count must not be negative: " + count);
        }
        this.count = count;
        this.upperBound = upperBound;
    }

    /**
     * The actual count must be exactly {@code count}.
     */
    public static ExpectedCount exactly(long count) {
        return new ExpectedCount(count, false);
    }

    /**
     * The actual count must be {@code count} or fewer.
     */
    public static ExpectedCount atMost(long count) {
        return new ExpectedCount(count, true);
    }

    boolean matches(long actualCount) {
        return upperBound ? actualCount <= count : actualCount == count;
    }

    /**
     * Describes the expectation the way it reads in a failure message, such as
     * {@code expected 3} or {@code expected at most 3}.
     */
    String describe() {
        return upperBound ? "expected at most " + count : "expected " + count;
    }

    @Override
    public String toString() {
        return describe();
    }

}
