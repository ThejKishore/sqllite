package com.tk.learn.vaadindemo.dbhelper;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DynamicRowMapper implements RowMapper<Map<String, Object>> {

    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> rowMap = new HashMap<>();

        // Get the number of columns in the result set
        int columnCount = rs.getMetaData().getColumnCount();

        // Loop through all columns and add them to the HashMap
        for (int i = 1; i <= columnCount; i++) {
            String columnName = rs.getMetaData().getColumnLabel(i);
            Object columnValue = rs.getObject(i);  // Get the column value by index
            rowMap.put(columnName, columnValue);  // Store the column name and value in the map
        }

        return rowMap;
    }
}
