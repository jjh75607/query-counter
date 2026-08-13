package soon.springtestutil.querycount.datasource;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import soon.springtestutil.core.context.TestContextHolder;
import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.ArrayList;
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
                    QueryCountContext.addQuery(queryType, sql, elapsedMs, extractParameters(queryInfo));
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

    /**
     * 바인딩된 파라미터 값을 꺼냅니다. setter 인자가 (인덱스, 값) 이라 두 번째가 값입니다.
     *
     * <p>세트가 여럿이면 배치입니다. 이 경우 분석을 멈추지 않고 세트를 그대로 넘깁니다.
     */
    private List<List<Object>> extractParameters(QueryInfo queryInfo) {
        List<List<ParameterSetOperation>> parametersList = queryInfo.getParametersList();
        if (parametersList == null || parametersList.isEmpty()) {
            return List.of();
        }

        List<List<Object>> sets = new ArrayList<>();
        for (List<ParameterSetOperation> operations : parametersList) {
            List<Object> values = new ArrayList<>();
            for (ParameterSetOperation operation : operations) {
                Object[] args = operation.getArgs();
                values.add(args != null && args.length > 1 ? args[1] : null);
            }
            sets.add(values);
        }
        return sets;
    }

}
