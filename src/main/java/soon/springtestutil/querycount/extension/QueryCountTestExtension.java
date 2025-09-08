package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.context.QueryCountContext;

public class QueryCountTestExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String className = context.getRequiredTestClass().getName();
        String methodName = context.getRequiredTestMethod().getName();
        TestContextHolder.setContext(className, methodName);

        QueryCountContext.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        TestContextHolder.clearContext();

        QueryCountContext.clear();
    }

}