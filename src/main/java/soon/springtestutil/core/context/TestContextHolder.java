package soon.springtestutil.core.context;

public class TestContextHolder {

    private static final ThreadLocal<String> testClassNameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> testMethodNameHolder = new ThreadLocal<>();

    public static void setContext(String className, String methodName) {
        testClassNameHolder.set(className);
        testMethodNameHolder.set(methodName);
    }

    /**
     * 지금 스레드가 테스트 스레드인지 알려줍니다.
     *
     * <p>테스트가 시작할 때 리스너나 확장이 이 값을 넣습니다. 톰캣 워커처럼 테스트가 만들지
     * 않은 스레드에서는 비어 있으므로, 쿼리를 어디에 담을지 가르는 신호로 씁니다.
     */
    public static boolean isInTest() {
        return testClassNameHolder.get() != null;
    }

    public static String getContextInfo() {
        String className = testClassNameHolder.get();
        String methodName = testMethodNameHolder.get();

        if (className != null && methodName != null) {
            return String.format("[Test: %s#%s] ", className, methodName);
        }

        return "";
    }

    public static void clearContext() {
        testClassNameHolder.remove();
        testMethodNameHolder.remove();
    }

}
