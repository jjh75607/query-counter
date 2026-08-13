package soon.springtestutil.querycount.context;

import lombok.Getter;
import soon.springtestutil.querycount.QueryType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class QueryInfo {

    // 부정 룩어헤드는 H2 의 select ... from final table (insert ...) 에서 final 을 테이블
    // 이름으로 뽑지 않기 위한 것이다. 안쪽의 into member 는 그대로 잡힌다.
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
        "\\b(from|join|into|update|delete\\s+from)\\s+(?!(?:final|new|old)\\s+table\\s*\\()([a-zA-Z0-9_`.\"]+)",
        Pattern.CASE_INSENSITIVE
    );

    private final QueryType queryType;
    private final String query;
    private final Set<String> tableNames;
    private final Long executionTimeMs;

    /**
     * 한 번의 실행에 바인딩된 파라미터 값이다. 바깥 목록은 파라미터 세트, 안쪽은 그 세트의
     * 값들이다. 배치로 나가면 세트가 여럿이지만 왕복은 한 번이므로 실행 하나로 남는다.
     */
    private final List<List<Object>> parameters;

    public QueryInfo(QueryType queryType, String query) {
        this(queryType, query, null);
    }

    public QueryInfo(QueryType queryType, String query, Long executionTimeMs) {
        this(queryType, query, executionTimeMs, Collections.emptyList());
    }

    public QueryInfo(QueryType queryType, String query, Long executionTimeMs, List<List<Object>> parameters) {
        this.queryType = queryType;
        this.query = query;
        this.tableNames = extractTableNames(query);
        this.executionTimeMs = executionTimeMs;
        this.parameters = copyOf(parameters);
    }

    private List<List<Object>> copyOf(List<List<Object>> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyList();
        }
        return parameters.stream()
            .map(set -> set == null ? Collections.emptyList() : Collections.unmodifiableList(set))
            .map(set -> (List<Object>) set)
            .toList();
    }

    private Set<String> extractTableNames(String query) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = TABLE_NAME_PATTERN.matcher(query);
        while (matcher.find()) {
            names.add(matcher.group(2)); // group(1) : keyword, group(2) : table name
        }
        return names;
    }

}
