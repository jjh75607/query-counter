package soon.springtestutil.querycount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueryTypeTest {

    @DisplayName("SQL의 시작 키워드에 대해 올바른 QueryType을 반환한다")
    @Test
    void fromSelectQueryShouldReturnSelectType() {
        // given
        String query = "SELECT * FROM users join orders ON users.id = orders.user_id";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.SELECT);
    }

    @DisplayName("알 수 없는 키워드에 대해 OTHERS를 반환한다.")
    @Test
    void fromUnknownQueryReturnsOthersType() {
        // given
        String query = "CREATE TABLE new_table (id INT)";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.OTHERS);
    }

    @DisplayName("대소문자가 혼합된 쿼리에 대해 올바른 타입을 반환한다")
    @Test
    void fromQueryWithMixedCaseShouldReturnCorrectType() {
        // given
        String query = "sElEcT name FROM customers";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.SELECT);
    }


    @DisplayName("쿼리가 null인 경우 예외가 발생한다.")
    @Test
    void fromNullQueryShouldThrowException() {
        // given
        String query = null;

        // expected
        assertThatThrownBy(() -> QueryType.from(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Query string cannot be null.");
    }

    @DisplayName("쿼리가 빈 문자열인 경우 예외가 발생한다.")
    @Test
    void fromEmptyQueryShouldThrowException() {
        // given
        String query = "";

        // when & then
        assertThatThrownBy(() -> QueryType.from(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Query string cannot be empty or consist only of whitespace.");
    }

    @DisplayName("쿼리가 공백인 경우 예외가 발생한다.")
    @Test
    void fromBlankQueryShouldThrowException() {
        // given
        String query = "   ";

        // when & then
        assertThatThrownBy(() -> QueryType.from(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Query string cannot be empty or consist only of whitespace.");
    }

    @DisplayName("블록 주석이 앞에 붙어도 본문의 타입으로 판정한다")
    @Test
    void fromQueryWithLeadingBlockCommentShouldReturnBodyType() {
        // given
        String query = "/* my comment */ select count(*) from member";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.SELECT);
    }

    @DisplayName("줄 주석이 앞에 붙어도 본문의 타입으로 판정한다")
    @Test
    void fromQueryWithLeadingLineCommentShouldReturnBodyType() {
        // given
        String query = "-- my comment\ninsert into member (name) values ('a')";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.INSERT);
    }

    @DisplayName("주석이 여러 개 겹쳐 붙어도 본문의 타입으로 판정한다")
    @Test
    void fromQueryWithMultipleLeadingCommentsShouldReturnBodyType() {
        // given
        String query = "/* one */ -- two\n /* three */ update member set name = 'a'";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.UPDATE);
    }

    @DisplayName("CTE 로 시작하면 본문의 첫 키워드로 판정한다")
    @Test
    void fromQueryWithCteShouldReturnBodyType() {
        // given
        String query = "with t as (select id from member) select count(*) from t";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.SELECT);
    }

    @DisplayName("CTE 의 본문이 조회가 아니면 그 본문의 타입으로 판정한다")
    @Test
    void fromQueryWithCteAndNonSelectBodyShouldReturnBodyType() {
        // given
        String query = "with t as (select id from member) insert into archive (id) select id from t";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.INSERT);
    }

    @DisplayName("재귀 CTE 도 본문의 첫 키워드로 판정한다")
    @Test
    void fromRecursiveCteShouldReturnBodyType() {
        // given
        String query =
            "with recursive t as (select id from member) delete from member where id in (select id from t)";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.DELETE);
    }

    @DisplayName("CTE 정의 안의 키워드는 본문으로 오인하지 않는다")
    @Test
    void fromCteShouldIgnoreKeywordsInsideDefinition() {
        // given
        String query = "with t as (select id from member where name = 'x') update member set name = 'y'";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.UPDATE);
    }

    @DisplayName("문자열 리터럴 안의 키워드는 본문으로 오인하지 않는다")
    @Test
    void fromCteShouldIgnoreKeywordsInsideStringLiteral() {
        // given
        String query = "with t as (select id from member) select name from t where name = 'delete me'";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.SELECT);
    }

    @DisplayName("주석과 CTE 가 함께 붙어도 본문의 타입으로 판정한다")
    @Test
    void fromQueryWithLeadingCommentAndCteShouldReturnBodyType() {
        // given
        String query = "/* comment */ with t as (select id from member) select count(*) from t";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.SELECT);
    }

    @DisplayName("CTE 뒤에 아는 키워드가 없으면 OTHERS 를 반환한다")
    @Test
    void fromCteWithoutKnownBodyKeywordShouldReturnOthers() {
        // given
        String query = "with t as (select id from member) merge into member using t on (member.id = t.id)";

        // when
        QueryType actualType = QueryType.from(query);

        // then
        assertThat(actualType).isEqualTo(QueryType.OTHERS);
    }

    @DisplayName("주석만 있고 본문이 없으면 예외가 발생한다")
    @Test
    void fromQueryWithOnlyCommentShouldThrowException() {
        // given
        String query = "/* only a comment */";

        // when & then
        assertThatThrownBy(() -> QueryType.from(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Query string cannot be empty or consist only of whitespace.");
    }

}
