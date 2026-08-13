package soon.springtestutil.querycount.assertion;

import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 같은 SELECT 가 서로 다른 파라미터 값으로 반복 실행됐는지 찾습니다.
 *
 * <p>파라미터까지 같은 반복은 N+1 이 아니라 중복 조회이므로 세지 않습니다. 값이 달라야
 * 부모 한 건마다 자식을 한 번씩 읽은 모양이 됩니다.
 */
final class NPlusOneDetector {

    private NPlusOneDetector() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * @param sql 반복 실행된 SELECT
     * @param executionCount 실행 횟수
     * @param distinctParameters 서로 달랐던 파라미터 값들
     */
    record Finding(String sql, int executionCount, List<List<List<Object>>> distinctParameters) {
    }

    static List<Finding> detect(List<QueryInfo> queries) {
        Map<String, Set<List<List<Object>>>> distinctParametersBySql = new LinkedHashMap<>();
        Map<String, Integer> executionCountBySql = new LinkedHashMap<>();

        for (QueryInfo query : queries) {
            if (query.getQueryType() != QueryType.SELECT) {
                continue;
            }
            distinctParametersBySql
                .computeIfAbsent(query.getQuery(), sql -> new LinkedHashSet<>())
                .add(query.getParameters());
            executionCountBySql.merge(query.getQuery(), 1, Integer::sum);
        }

        List<Finding> findings = new ArrayList<>();
        distinctParametersBySql.forEach((sql, parameters) -> {
            if (parameters.size() > 1) {
                findings.add(new Finding(sql, executionCountBySql.get(sql), List.copyOf(parameters)));
            }
        });
        return findings;
    }

}
