package soon.springtestutil.querycount;

import lombok.Getter;

@Getter
public class TestContextHolder {

    private static final ThreadLocal<String> testClassNameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> testMethodNameHolder = new ThreadLocal<>();

    public static void setContext(String className, String methodName) {
        testClassNameHolder.set(className);
        testMethodNameHolder.set(methodName);
    }

    public static String getContextInfo() {
        String className = testClassNameHolder.get();
        String methodName = testMethodNameHolder.get();

        if (className != null && methodName != null) {
            return String.format("[Test: %s#%s]", className, methodName);
        }

        return "[Test: Unknown]";
    }

    public static void clearContext() {
        testClassNameHolder.remove();
        testMethodNameHolder.remove();
    }

}