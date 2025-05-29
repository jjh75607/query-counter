package soon.springtestutil.querycount;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueryType {

    SELECT("SELECT"),
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    OTHERS("OTHERS"); // OTHERS는 알 수 없는 쿼리 유형을 나타냅니다.

    private final String keyword;

    public static QueryType from(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Query string cannot be null.");
        }
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            throw new IllegalArgumentException("Query string cannot be empty or consist only of whitespace.");
        }

        String upperQuery = trimmedQuery.toUpperCase();

        return Arrays.stream(values())
            .filter(type -> type != OTHERS && upperQuery.startsWith(type.keyword))
            .findFirst()
            .orElse(OTHERS);
    }

}