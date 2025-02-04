package com.tk.learn.vaadindemo.dbhelper;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Route("")
@PageTitle("dynamic-grid")
public class DynamicGridView extends VerticalLayout {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;
    private ComboBox<String> tableSelector;
    private Grid<Map<String, Object>> grid = new Grid<>();
    private FormLayout dynamicForm = new FormLayout();
//    private FormLayout filterForm = new FormLayout();  // Form for filtering columns
    private Button updateButton = new Button("Update");
//    private Button insertButton = new Button("Insert New Record");
    private Map<String, Object> selectedRow;
    private Set<String> editableColumns = new HashSet<>();
    private Set<String> primaryKeyColumns = new HashSet<>();
    private Map<String,Integer> columnNameToTypes = new HashMap<>();
    private Map<String, TextField> filterFields = new HashMap<>(); // For dynamic filters

    private String queryToFetchAllTables = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";

    @Autowired
    public DynamicGridView(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;

        // Set up the table selector combo box
        tableSelector = new ComboBox<>("Select Table");
        tableSelector.setItems(getTableNames());
        tableSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                updateGrid(event.getValue());
                columnNameToTypes.clear();
                columnNameToTypes = getColumnToType(event.getValue());

//                insertButton.setVisible(true);
                dynamicForm.setVisible(true);
//                filterForm.setVisible(true);  // Show filter form on table selection
            } else {
//                insertButton.setVisible(false);
                dynamicForm.setVisible(false);
//                filterForm.setVisible(false);  // Hide filter form if no table is selected
            }
        });

        // Set up the "Update" Button
        updateButton.setVisible(false);
        updateButton.addClickListener(event -> updateSelectedRow());

        // Set up the "Insert" Button
//        insertButton.setVisible(false);
//        insertButton.addClickListener(event -> insertNewRecord());

        // Add components to the layout
        add(tableSelector, grid, dynamicForm, updateButton);//, insertButton);//, filterForm);

        // Add row selection listener to the grid
        grid.addSelectionListener(event -> {
            if (event.getFirstSelectedItem().isPresent()) {
                Map<String, Object> selectedRow = event.getFirstSelectedItem().get();
                handleRowSelection(selectedRow);
            }
        });
    }

    // Fetch table names from the database
    private List<String> getTableNames() {
        return jdbcClient.sql(queryToFetchAllTables)
                .query(String.class)
                .list()
                .stream()
                .filter(this::isValidTableName)
                .collect(Collectors.toList());
    }

    // Validate table name to avoid SQL injection
    private boolean isValidTableName(String tableName) {
        return tableName != null && tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    // Update the grid based on selected table
    private void updateGrid(String tableName) {
        try {
            primaryKeyColumns = getPrimaryKeyColumns(tableName);

            List<Map<String, Object>> rows = getTableData(tableName);
            if (rows == null || rows.isEmpty()) {
                grid.setVisible(false);
                return;
            }

            // Set up filtering fields dynamically
//            filterForm.removeAll();
            filterFields.clear();
            Map<String, Object> firstRow = rows.get(0);
            firstRow.keySet().forEach(columnName -> {
                // Add filter input for each column
                TextField filterField = new TextField();
                filterField.setPlaceholder("Filter " + columnName);
                filterField.addValueChangeListener(event -> applyFilters(tableName));
                filterFields.put(columnName, filterField);
//                filterForm.add(filterField);
            });

            grid.removeAllColumns();
            firstRow.keySet().forEach(columnName -> grid.addColumn(item -> item.get(columnName)).setHeader(columnName));
            grid.setItems(rows);
            grid.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error loading data: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    // Fetch primary key columns for the selected table
    private Set<String> getPrimaryKeyColumns(String tableName) {
        Set<String> pkColumns = new HashSet<>();
        try {
            String query = "SELECT column_name FROM information_schema.key_column_usage " +
                    "WHERE table_name = ?";

            List<String> pkColumnsList = jdbcClient.sql(query)
                    .params(tableName)
                    .query(String.class)
                    .list();

            pkColumns.addAll(pkColumnsList);
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error fetching primary key columns: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
        return pkColumns;
    }

    // Fetch rows for the selected table
    private List<Map<String, Object>> getTableData(String tableName) {
        return jdbcClient.sql("SELECT * FROM " + tableName)
                .query(new DynamicRowMapper())
                .list();
    }

    // Handle row selection in the grid
    private void handleRowSelection(Map<String, Object> selectedRow) {
        this.selectedRow = selectedRow;

        // Clear any previous form fields
        dynamicForm.removeAll();
        editableColumns.clear(); // Clear previous editable columns

        // Dynamically generate form fields for the selected row's columns
        if (selectedRow != null && !selectedRow.isEmpty()) {
            selectedRow.forEach((column, value) -> {
                boolean isDateTimeColumn = isDateOrTimeColumn(column);
                if (primaryKeyColumns.contains(column) || isDateTimeColumn) {
                    return;  // Do not add the primary key column or date/time columns to the form
                }

                TextField textField = new TextField(column);
                textField.setValue(value != null ? value.toString() : "");
                dynamicForm.add(textField);  // Add the TextField to the form
                editableColumns.add(column); // Track editable columns
            });

            updateButton.setVisible(true);  // Show the update button
        }
    }

    private boolean isDateOrTimeColumn(String column) {
        return switch (columnNameToTypes.get(column)){
            case Types.DATE,Types.TIME,Types.TIMESTAMP,Types.TIME_WITH_TIMEZONE,Types.TIMESTAMP_WITH_TIMEZONE -> true;
            default -> false;
        };
    }

    // Helper method to check if the column type is DATE, TIME or TIMESTAMP
    private Map<String,Integer> getColumnToType(String tableName) {
        Map<String,Integer> columnToType = new HashMap<>();
        try {
            String query = "SELECT * FROM " + tableName + " LIMIT 1";  // Fetch one row for metadata
            jdbcTemplate.query(query, new RowMapper<Object>() {
                @Override
                public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                    ResultSetMetaData metaData = rs.getMetaData();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        String column = metaData.getColumnName(i);
                        int columnType = metaData.getColumnType(i);
                        columnToType.put(column, columnType);
                    }
                    return columnToType;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return columnToType;
    }

    // Apply filters to the grid data
    private void applyFilters(String tableName) {
        try {
            // Create a base SQL query
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1=1");

            // Add filtering conditions based on user input
            List<Object> params = new ArrayList<>();
            filterFields.forEach((column, filterField) -> {
                String filterValue = filterField.getValue();
                if (filterValue != null && !filterValue.isEmpty()) {
                    queryBuilder.append(" AND " + column + " LIKE ?");
                    params.add("%" + filterValue + "%");
                }
            });

            // Execute the filtered query
            List<Map<String, Object>> filteredRows = jdbcClient.sql(queryBuilder.toString())
                    .params(params.toArray())
                    .query(new DynamicRowMapper())
                    .list();

            grid.setItems(filteredRows);
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error applying filters: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    // Insert a new record into the selected table
    private void insertNewRecord() {
        if (selectedRow != null && !selectedRow.isEmpty()) {
            try {
                String tableName = tableSelector.getValue();

                List<TextField> fields = dynamicForm.getChildren()
                        .map(component -> (TextField) component)
                        .collect(Collectors.toList());

                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");
                StringBuilder valuesClause = new StringBuilder("VALUES (");

                List<Object> params = new ArrayList<>();
                int i = 0;
                for (TextField field : fields) {
                    String columnName = field.getLabel();
                    String newValue = field.getValue();

                    insertQuery.append(columnName);
                    valuesClause.append("?");
                    params.add(newValue);

                    if (i < fields.size() - 1) {
                        insertQuery.append(", ");
                        valuesClause.append(", ");
                    }
                    i++;
                }

                insertQuery.append(")");
                valuesClause.append(")");

                // Combine the query and the values clause
                insertQuery.append(valuesClause);

                // Execute the insert query
                jdbcClient.sql(insertQuery.toString())
                        .params(params.toArray())
                        .update();

                // Reload the grid to reflect the changes
                updateGrid(tableName);
                Notification.show("New record inserted successfully.", 3000, Notification.Position.MIDDLE);

                // Clear the form after insertion
                dynamicForm.removeAll();
            } catch (Exception e) {
                e.printStackTrace();
                Notification.show("Error inserting data: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        }
    }

    // Update the selected row in the database
    private void updateSelectedRow() {
        if (selectedRow != null && !selectedRow.isEmpty()) {
            try {
                String tableName = tableSelector.getValue();

                // Collect the updated values from the form
                List<TextField> fields = dynamicForm.getChildren().map(component -> (TextField) component).collect(Collectors.toList());

                // Build the SQL update query dynamically based on the selected columns
                StringBuilder updateQuery = new StringBuilder("UPDATE " + tableName + " SET ");
                StringBuilder whereClause = new StringBuilder(" WHERE ");

                // Collect column names and values
                List<Object> params = new ArrayList<>();
                int i = 0;
                for (TextField field : fields) {
                    String columnName = field.getLabel();
                    String newValue = field.getValue();

                    updateQuery.append(columnName + " = ?");
                    if (i < fields.size() - 1) {
                        updateQuery.append(", ");
                    }

                    params.add(newValue); // Add the updated value
                    i++;
                }

                // Append the WHERE clause with primary key condition(s)
                if (!primaryKeyColumns.isEmpty()) {
                    String pkColumn = primaryKeyColumns.iterator().next();
                    whereClause.append(pkColumn + " = ?");
                    params.add(selectedRow.get(pkColumn)); // Add the value of the primary key from the selected row
                }

                // Combine the query and where clause
                updateQuery.append(whereClause);

                // Execute the update query
                jdbcClient.sql(updateQuery.toString())
                        .params(params.toArray())  // Pass the parameters to the query
                        .update();

                // Reload the grid to reflect the changes
                updateGrid(tableName);
                Notification.show("Row updated successfully.", 3000, Notification.Position.MIDDLE);

                // Clear the form and hide the update button
                dynamicForm.removeAll();
                updateButton.setVisible(false);

            } catch (Exception e) {
                e.printStackTrace();
                Notification.show("Error updating data: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        }
    }
}
