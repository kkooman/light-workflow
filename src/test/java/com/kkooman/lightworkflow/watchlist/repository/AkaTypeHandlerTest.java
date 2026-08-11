package com.kkooman.lightworkflow.watchlist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AkaTypeHandlerTest {
    private final AkaTypeHandler handler = new AkaTypeHandler();

    @Test
    void serializesAkaAsJsonForDatabase() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        handler.setNonNullParameter(statement, 2, List.of("홍길동", "Hong Gil Dong"), null);

        verify(statement).setString(2, "[\"홍길동\",\"Hong Gil Dong\"]");
    }

    @Test
    void parsesJsonAndHandlesNullOrBlankValues() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("aka")).thenReturn("[\"A\",\"B\"]");

        assertThat(handler.getNullableResult(resultSet, "aka")).containsExactly("A", "B");
        when(resultSet.getString("aka")).thenReturn(null);
        assertThat(handler.getNullableResult(resultSet, "aka")).isEmpty();
        when(resultSet.getString("aka")).thenReturn("   ");
        assertThat(handler.getNullableResult(resultSet, "aka")).isEmpty();
    }

    @Test
    void rejectsMalformedAkaJson() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(1)).thenReturn("{not-json}");

        assertThatThrownBy(() -> handler.getNullableResult(resultSet, 1))
                .isInstanceOf(SQLException.class)
                .hasMessage("AKA value could not be parsed");
    }
}
