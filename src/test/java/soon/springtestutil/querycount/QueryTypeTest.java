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

}