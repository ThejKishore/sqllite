package com.tk.learn.vaadindemo.sqlhelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SqlExecutionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] FORBIDDEN_COMMANDS = {"DROP", "TRUNCATE", "DELETE"};

    /**
     * Executes the provided SQL query after validating it.
     *
     * @param sql The SQL query to execute
     * @return The result of the query if valid
     * @throws IllegalArgumentException If the query contains forbidden SQL commands
     */
    public List<Map<String, Object>> executeSql(String sql) throws IllegalArgumentException {
        // Check for forbidden SQL commands
        if (containsForbiddenSql(sql)) {
            throw new IllegalArgumentException("The query contains forbidden SQL commands (DROP, TRUNCATE, DELETE).");
        }

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Checks if the query contains any forbidden SQL commands (DROP, TRUNCATE, DELETE).
     *
     * @param sql The SQL query to check
     * @return true if the query contains forbidden commands, false otherwise
     */
    private boolean containsForbiddenSql(String sql) {
        // Convert the SQL query to uppercase for case-insensitive comparison
        sql = sql.toUpperCase().trim();

        // Check if the query contains any forbidden command
        for (String forbiddenCommand : FORBIDDEN_COMMANDS) {
            if (sql.contains(forbiddenCommand)) {
                return true;
            }
        }
        return false;
    }
}
