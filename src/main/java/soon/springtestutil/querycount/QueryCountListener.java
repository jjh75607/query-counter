package soon.springtestutil.querycount;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

@Slf4j
public class QueryCountListener implements QueryExecutionListener {

    private static final ConcurrentMap<String, QueryType> queryTypeCache = new ConcurrentHashMap<>();

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        for (QueryInfo queryInfo : queryInfoList) {
            String sql = queryInfo.getQuery();
            if (sql != null && !sql.trim().isEmpty()) {
                try {
                    QueryType queryType = queryTypeCache.computeIfAbsent(sql, QueryType::from);
                    QueryCountContext.increment(queryType);
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