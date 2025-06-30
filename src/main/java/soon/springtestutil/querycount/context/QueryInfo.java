package soon.springtestutil.querycount.context;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import soon.springtestutil.querycount.QueryType;

@Getter
public class QueryInfo {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
        "\\b(from|join|into|update|delete\\s+from)\\s+([a-zA-Z0-9_`.\"]+)",
        Pattern.CASE_INSENSITIVE
    );

    private final QueryType queryType;
    private final String query;
    private final Set<String> tableNames;

    public QueryInfo(QueryType queryType, String query) {
        this.queryType = queryType;
        this.query = query;
        this.tableNames = extractTableNames(query);
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