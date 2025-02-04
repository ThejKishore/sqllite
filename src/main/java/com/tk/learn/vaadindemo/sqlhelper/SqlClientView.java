package com.tk.learn.vaadindemo.sqlhelper;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route("sqllite")
@PageTitle("dynamic-grid-sql-lite")
public class SqlClientView extends VerticalLayout {

    private final SqlExecutionService sqlExecutionService;

    private TextArea sqlTextArea;
    private Button executeButton;
    private Grid<Map<String, Object>> resultGrid;

    @Autowired
    public SqlClientView(SqlExecutionService sqlExecutionService) {
        this.sqlExecutionService = sqlExecutionService;

        // Create UI components
        sqlTextArea = new TextArea("SQL Query");
        sqlTextArea.setWidth("100%");
        sqlTextArea.setHeight("350px");

        executeButton = new Button("Execute SQL");
        resultGrid = new Grid<>();

        // Add listener for execute button
        executeButton.addClickListener(e -> executeSql());

        // Layout the components
        add(sqlTextArea, executeButton, resultGrid);
    }

    private void executeSql() {
        String sql = sqlTextArea.getValue();

        if (sql == null || sql.trim().isEmpty()) {
            Notification.show("Please enter a valid SQL query.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            List<Map<String, Object>> results = sqlExecutionService.executeSql(sql);
            displayResults(results);
        } catch (Exception e) {
            Notification.show("Error executing query: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void displayResults(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            Notification.show("No results found.", 3000, Notification.Position.MIDDLE);
            resultGrid.setItems(results);
            return;
        }

        // Dynamically create columns based on query result
        resultGrid.removeAllColumns();
        Map<String, Object> firstRow = results.get(0);
        firstRow.keySet().forEach(columnName -> resultGrid.addColumn(row -> row.get(columnName)).setHeader(columnName));
        resultGrid.setItems(results);
    }
}
