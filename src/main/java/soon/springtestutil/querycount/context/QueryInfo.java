package soon.springtestutil.querycount.context;

import lombok.Getter;
import soon.springtestutil.querycount.QueryType;

import java.util.LinkedHashSet;
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

    public QueryInfo(QueryType queryType, String query) {
        this(queryType, query, null);
    }

    public QueryInfo(QueryType queryType, String query, Long executionTimeMs) {
        this.queryType = queryType;
        this.query = query;
        this.tableNames = extractTableNames(query);
        this.executionTimeMs = executionTimeMs;
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
