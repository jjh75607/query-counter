package soon.springtestutil.querycount;

/**
 * How many queries one test may run, and whether to report what it ran.
 *
 * <p>Set with {@code query-counter.max-queries.per-test} and
 * {@code query-counter.max-queries.report}. Both are off by default, so adding this library
 * cannot change how existing tests behave.
 *
 * <p>A limit catches a test that suddenly runs hundreds of queries, which is what a loop
 * around a repository call looks like. It does not catch a count creeping from 3 to 12.
 *
 * @param maxPerTest the limit, or 0 for no limit
 * @param report whether to log the query count of every test
 */
public record QueryLimit(int maxPerTest, boolean report) {

    /** No limit and no report. */
    public static final QueryLimit OFF = new QueryLimit(0, false);

    public static QueryLimit of(int maxPerTest, boolean report) {
        if (maxPerTest <= 0 && !report) {
            return OFF;
        }
        return new QueryLimit(Math.max(maxPerTest, 0), report);
    }

    /** Whether anything needs to happen at the end of a test. */
    public boolean isActive() {
        return this.maxPerTest > 0 || this.report;
    }

    public boolean exceededBy(long total) {
        return this.maxPerTest > 0 && total > this.maxPerTest;
    }

}
