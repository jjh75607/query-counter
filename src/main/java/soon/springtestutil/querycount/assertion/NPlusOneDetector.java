package soon.springtestutil.querycount.assertion;

import soon.springtestutil.querycount.QueryType;
import soon.springtestutil.querycount.context.QueryInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final int MAX_FINDINGS_TO_REPORT = 3;

    /**
     * 찾은 것을 사람이 읽을 문장으로 만듭니다. 어서션 실패와 전역 검사가 같은 형식을
     * 쓰도록 여기 둡니다.
     *
     * @param headline 첫 줄. 실패인지 보고인지에 따라 달라집니다
     */
    static String format(List<Finding> findings, String headline) {
        StringBuilder sb = new StringBuilder(headline);

        int reportCount = Math.min(findings.size(), MAX_FINDINGS_TO_REPORT);
        for (int i = 0; i < reportCount; i++) {
            Finding finding = findings.get(i);
            sb.append(String.format("\n[%d] %d executions, %d distinct parameter values\n    SQL: %s\n    params: %s",
                i + 1,
                finding.executionCount(),
                finding.distinctParameters().size(),
                finding.sql(),
                describeParameters(finding.distinctParameters())));
        }

        if (findings.size() > MAX_FINDINGS_TO_REPORT) {
            sb.append(String.format("\n... and %d more", findings.size() - MAX_FINDINGS_TO_REPORT));
        }

        return sb.toString();
    }

    private static String describeParameters(List<List<List<Object>>> distinctParameters) {
        return distinctParameters.stream()
            .limit(MAX_FINDINGS_TO_REPORT)
            .map(sets -> sets.size() == 1 ? sets.get(0).toString() : sets.toString())
            .collect(Collectors.joining(", "))
            + (distinctParameters.size() > MAX_FINDINGS_TO_REPORT ? ", ..." : "");
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
