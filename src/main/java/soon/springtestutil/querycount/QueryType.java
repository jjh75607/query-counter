package soon.springtestutil.querycount;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueryType {

    SELECT("SELECT"),
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    OTHERS("OTHERS"); // Any statement that is not one of the four above.

    private final String keyword;

    private static final String CTE_KEYWORD = "WITH";

    private static final Pattern DATA_CHANGE_DELTA_TABLE_PATTERN = Pattern.compile(
        "\\bFROM\\s+(?:FINAL|NEW|OLD)\\s+TABLE\\s*\\("
    );

    public static QueryType from(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Query string cannot be null.");
        }
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            throw new IllegalArgumentException("Query string cannot be empty or consist only of whitespace.");
        }

        String statement = stripLeadingComments(trimmedQuery);
        if (statement.isEmpty()) {
            throw new IllegalArgumentException("Query string cannot be empty or consist only of whitespace.");
        }

        String upperQuery = statement.toUpperCase();

        if (startsWithKeyword(upperQuery, CTE_KEYWORD)) {
            return fromCteBody(upperQuery);
        }

        QueryType type = Arrays.stream(values())
            .filter(candidate -> candidate != OTHERS && upperQuery.startsWith(candidate.keyword))
            .findFirst()
            .orElse(OTHERS);

        if (type == SELECT) {
            return fromDataChangeDeltaTable(upperQuery).orElse(SELECT);
        }

        return type;
    }

    // H2 는 자동 증가 키를 돌려줄 때 select ... from final table (insert ...) 형태를 보낸다.
    // 왕복은 1회이고 사용자가 의도한 동작은 저장이므로 안쪽 문장의 타입으로 센다.
    private static Optional<QueryType> fromDataChangeDeltaTable(String upperQuery) {
        Matcher matcher = DATA_CHANGE_DELTA_TABLE_PATTERN.matcher(upperQuery);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int cursor = matcher.end();
        while (cursor < upperQuery.length() && Character.isWhitespace(upperQuery.charAt(cursor))) {
            cursor++;
        }
        final int innerStatementStart = cursor;

        return Arrays.stream(values())
            .filter(candidate -> candidate != OTHERS
                && matchesKeywordAt(upperQuery, innerStatementStart, candidate.keyword))
            .findFirst();
    }

    // hibernate.use_sql_comments=true 를 켜면 모든 SQL 앞에 주석이 붙는다.
    // 맨 앞에서만 벗기므로 리터럴 안의 /* 를 오인할 일은 없다.
    private static String stripLeadingComments(String query) {
        String remaining = query;

        while (true) {
            if (remaining.startsWith("/*")) {
                int end = remaining.indexOf("*/");
                if (end < 0) {
                    return "";
                }
                remaining = remaining.substring(end + 2).trim();
            } else if (remaining.startsWith("--")) {
                int end = remaining.indexOf('\n');
                if (end < 0) {
                    return "";
                }
                remaining = remaining.substring(end + 1).trim();
            } else {
                return remaining;
            }
        }
    }

    // with ... select 도 있고 with ... insert 도 있어 괄호 밖의 첫 키워드로 판정한다.
    // 파서는 넣지 않는다. 못 걸러지는 형태가 나오면 테스트로 쌓아 좁게 늘린다.
    private static QueryType fromCteBody(String upperQuery) {
        int depth = 0;
        boolean inStringLiteral = false;
        int index = CTE_KEYWORD.length();

        while (index < upperQuery.length()) {
            char current = upperQuery.charAt(index);

            if (inStringLiteral) {
                inStringLiteral = current != '\'';
                index++;
                continue;
            }

            if (current == '\'') {
                inStringLiteral = true;
                index++;
                continue;
            }

            if (current == '(') {
                depth++;
                index++;
                continue;
            }

            if (current == ')') {
                depth--;
                index++;
                continue;
            }

            if (depth == 0 && isWordStart(upperQuery, index)) {
                for (QueryType type : values()) {
                    if (type != OTHERS && matchesKeywordAt(upperQuery, index, type.keyword)) {
                        return type;
                    }
                }
                index = skipWord(upperQuery, index);
                continue;
            }

            index++;
        }

        return OTHERS;
    }

    private static boolean startsWithKeyword(String upperQuery, String keyword) {
        return matchesKeywordAt(upperQuery, 0, keyword);
    }

    private static boolean matchesKeywordAt(String upperQuery, int index, String keyword) {
        if (!upperQuery.startsWith(keyword, index)) {
            return false;
        }
        int next = index + keyword.length();
        return next >= upperQuery.length() || !isWordChar(upperQuery.charAt(next));
    }

    private static boolean isWordStart(String upperQuery, int index) {
        if (!Character.isLetter(upperQuery.charAt(index))) {
            return false;
        }
        return index == 0 || !isWordChar(upperQuery.charAt(index - 1));
    }

    private static int skipWord(String upperQuery, int index) {
        int next = index;
        while (next < upperQuery.length() && isWordChar(upperQuery.charAt(next))) {
            next++;
        }
        return next;
    }

    private static boolean isWordChar(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

}
