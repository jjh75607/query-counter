package soon.springtestutil.querycount.datasource;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class QueryCountListener implements QueryExecutionListener {

    private static final ConcurrentMap<String, QueryType> queryTypeCache = new ConcurrentHashMap<>();

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Long elapsedMs = null;
        try {
            if (execInfo != null) {
                elapsedMs = execInfo.getElapsedTime();
            }
        } catch (Exception e) {
            log.debug("Failed to get elapsed time from ExecutionInfo: {}", e.getMessage());
        }

        for (QueryInfo queryInfo : queryInfoList) {
            String sql = queryInfo.getQuery();
            if (sql != null && !sql.trim().isEmpty()) {
                try {
                    QueryType queryType = queryTypeCache.computeIfAbsent(sql, QueryType::from);
                    QueryCountContext.addQuery(queryType, sql, elapsedMs);
                } catch (IllegalArgumentException e) {
                    String contextInfo = TestContextHolder.getContextInfo();
                    log.warn("{}Cannot determine query type for: [{}]. Error: {}",
                        contextInfo,
                        sql,
                        e.getMessage()
                    );
                }
            }
        }
    }

}
