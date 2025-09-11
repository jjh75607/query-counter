package soon.springtestutil.querycount.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import soon.springtestutil.querycount.QueryType;

import static org.assertj.core.api.Assertions.assertThat;

class QueryInfoTest {

    @DisplayName("SELECT 구문에서 테이블 명을 추출한다.")
    @Test
    void extractTableName() {
        // given
        String sql = "SELECT * FROM member";

        // when
        QueryInfo info = new QueryInfo(QueryType.SELECT, sql);

        // then
        assertThat(info.getTableNames()).containsExactly("member");
    }

    @Test
    @DisplayName("같은 테이블 명이 반복되어도 한 번만 포함된다")
    void duplicateTableNamesAreDeduplicated() {
        // given
        String sql = "SELECT * FROM member m JOIN member m2 ON m.id = m2.ref_id";

        // when
        QueryInfo info = new QueryInfo(QueryType.SELECT, sql);

        // then
        assertThat(info.getTableNames()).containsExactly("member");
    }

    @Test
    @DisplayName("백틱 또는 따옴표로 감싼 테이블명을 추출한다")
    void extractQuotedTableName() {
        // given
        String sql1 = "INSERT INTO `member` (id) VALUES (1)";
        String sql2 = "SELECT * FROM \"MemberTable\"";
        String sql3 = "UPDATE `schema`.`member` SET name=?";

        // when
        QueryInfo info1 = new QueryInfo(QueryType.INSERT, sql1);
        QueryInfo info2 = new QueryInfo(QueryType.SELECT, sql2);
        QueryInfo info3 = new QueryInfo(QueryType.UPDATE, sql3);

        // then
        assertThat(info1.getTableNames()).containsExactly("`member`");
        assertThat(info2.getTableNames()).containsExactly("\"MemberTable\"");
        assertThat(info3.getTableNames()).containsExactly("`schema`.`member`");
    }

    @Test
    @DisplayName("DELETE 구문에서 테이블명을 추출한다")
    void extractTableNameFromDelete() {
        // given
        String sql = "DELETE FROM member WHERE id=?";

        // when
        QueryInfo info = new QueryInfo(QueryType.DELETE, sql);

        // then
        assertThat(info.getTableNames()).containsExactly("member");
    }

    @Test
    @DisplayName("대소문자 혼합 키워드에서도 테이블명을 추출한다")
    void extractTableNamesCaseInsensitive() {
        // given
        String sql = "select * FrOm Member m JoIn Orders o ON m.id=o.mid";

        // when
        QueryInfo info = new QueryInfo(QueryType.SELECT, sql);

        // then
        assertThat(info.getTableNames()).containsExactly("Member", "Orders");
    }

    @Test
    @DisplayName("매칭되는 키워드가 없으면 테이블명 집합은 비어 있다")
    void emptyTableNamesWhenNoKeywords() {
        // given
        String sql = "SELECT 1";

        // when
        QueryInfo info = new QueryInfo(QueryType.SELECT, sql);

        // then
        assertThat(info.getTableNames()).isEmpty();
    }

    @Test
    @DisplayName("기본 생성자를 사용하면 executionTimeMs는 null이다")
    void executionTimeIsNullWhenNotProvided() {
        // given
        String sql = "SELECT * FROM member";

        // when
        QueryInfo info = new QueryInfo(QueryType.SELECT, sql);

        // then
        assertThat(info.getExecutionTimeMs()).isNull();
    }

    @Test
    @DisplayName("여러 구문이 혼합된 SQL에서 모든 대상 테이블을 추출한다")
    void extractAllTablesFromComplexSql() {
        // given
        String sql = """
            UPDATE orders o
            JOIN member m ON o.member_id = m.id
            JOIN product p ON p.id = o.product_id
            SET o.status='DONE'
            """;

        // when
        QueryInfo info = new QueryInfo(QueryType.UPDATE, sql);

        // then
        assertThat(info.getTableNames()).containsExactly("orders", "member", "product");
    }

}