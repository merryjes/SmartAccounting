package com.smartaccounting.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FinancialAnalyticsPane {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/smart_accounting?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private final String BACKGROUND = "#17120F";
    private final String CARD = "#241B16";
    private final String BORDER = "#443329";
    private final String TEXT = "#F0E2D3";
    private final String MUTED = "#A99583";
    private final String ACCENT = "#C69A72";
    private final String GREEN = "#91B89A";
    private final String RED = "#C98278";

    private Label revenueValue;
    private Label expenseValue;
    private Label netIncomeValue;
    private Label profitMarginValue;

    private LineChart<String, Number> trendChart;
    private PieChart expenseChart;

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    private VBox content;

    public BorderPane build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");

        VBox page = new VBox(22);
        page.setPadding(new Insets(28));
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        page.getChildren().addAll(
                buildHeader(),
                buildFilters(),
                buildSummaryCards(),
                buildCharts(),
                buildInsightSection()
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: " + BACKGROUND + ";" +
                "-fx-background-color: " + BACKGROUND + ";" +
                "-fx-border-color: transparent;"
        );

        root.setCenter(scroll);

        setDefaultDates();
        refreshAnalytics();

        return root;
    }

    private VBox buildHeader() {
        VBox header = new VBox(7);

        Label eyebrow = new Label("FINANCIAL INTELLIGENCE");
        eyebrow.setStyle(
                "-fx-text-fill: " + ACCENT + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 2px;"
        );

        Label title = new Label("Financial Analytics");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 29));
        title.setTextFill(Color.web(TEXT));

        Label subtitle = new Label(
                "Understand your business performance through financial data."
        );
        subtitle.setTextFill(Color.web(MUTED));
        subtitle.setFont(Font.font("Roboto", 12));

        header.getChildren().addAll(eyebrow, title, subtitle);
        return header;
    }

    private HBox buildFilters() {
        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(15));
        filters.setStyle(cardStyle());

        VBox startBox = new VBox(6);
        Label startLabel = smallLabel("START DATE");
        startDatePicker = new DatePicker();
        styleDatePicker(startDatePicker);
        startBox.getChildren().addAll(startLabel, startDatePicker);

        VBox endBox = new VBox(6);
        Label endLabel = smallLabel("END DATE");
        endDatePicker = new DatePicker();
        styleDatePicker(endDatePicker);
        endBox.getChildren().addAll(endLabel, endDatePicker);

        Button apply = new Button("APPLY FILTER");
        apply.setStyle(buttonStyle(ACCENT, BACKGROUND));
        apply.setOnAction(e -> refreshAnalytics());

        Button month = new Button("THIS MONTH");
        month.setStyle(buttonStyle(CARD, TEXT));
        month.setOnAction(e -> {
            LocalDate now = LocalDate.now();
            startDatePicker.setValue(now.withDayOfMonth(1));
            endDatePicker.setValue(now);
            refreshAnalytics();
        });

        Button year = new Button("THIS YEAR");
        year.setStyle(buttonStyle(CARD, TEXT));
        year.setOnAction(e -> {
            LocalDate now = LocalDate.now();
            startDatePicker.setValue(now.withDayOfYear(1));
            endDatePicker.setValue(now);
            refreshAnalytics();
        });

        filters.getChildren().addAll(
                startBox, endBox, apply, month, year
        );

        return filters;
    }

    private GridPane buildSummaryCards() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        revenueValue = metricValue();
        expenseValue = metricValue();
        netIncomeValue = metricValue();
        profitMarginValue = metricValue();

        grid.add(metricCard("TOTAL REVENUE", revenueValue, GREEN,
                "Income earned during selected period"), 0, 0);

        grid.add(metricCard("TOTAL EXPENSES", expenseValue, RED,
                "Expenses recorded during selected period"), 1, 0);

        grid.add(metricCard("NET INCOME", netIncomeValue, ACCENT,
                "Revenue minus expenses"), 2, 0);

        grid.add(metricCard("PROFIT MARGIN", profitMarginValue, GREEN,
                "Net income as a percentage of revenue"), 3, 0);

        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(25);

        for (int i = 0; i < 4; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints());
            grid.getColumnConstraints().get(i).setPercentWidth(25);
        }

        return grid;
    }

    private VBox metricCard(String title, Label value, String accent, String description) {
        VBox card = new VBox(11);
        card.setPadding(new Insets(19));
        card.setStyle(cardStyle());

        Label heading = smallLabel(title);
        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setTextFill(Color.web(MUTED));
        descriptionLabel.setFont(Font.font("Roboto", 10));

        Region line = new Region();
        line.setPrefHeight(2);
        line.setMaxWidth(42);
        line.setStyle("-fx-background-color: " + accent + ";");

        card.getChildren().addAll(heading, value, line, descriptionLabel);
        return card;
    }

    private HBox buildCharts() {
        HBox charts = new HBox(15);
        charts.setAlignment(Pos.TOP_CENTER);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("MONTH");
        yAxis.setLabel("AMOUNT");

        trendChart = new LineChart<>(xAxis, yAxis);
        trendChart.setTitle("Revenue & Expense Trends");
        trendChart.setAnimated(false);
        trendChart.setCreateSymbols(true);
        trendChart.setLegendVisible(true);
        trendChart.setPrefHeight(360);
        trendChart.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        expenseChart = new PieChart();
        expenseChart.setTitle("Expense Distribution");
        expenseChart.setAnimated(false);
        expenseChart.setLegendVisible(true);
        expenseChart.setLabelsVisible(true);
        expenseChart.setPrefHeight(360);

        VBox trendCard = chartCard("PERFORMANCE TRENDS", trendChart);
        VBox expenseCard = chartCard("EXPENSE BREAKDOWN", expenseChart);

        HBox.setHgrow(trendCard, Priority.ALWAYS);
        HBox.setHgrow(expenseCard, Priority.ALWAYS);

        charts.getChildren().addAll(trendCard, expenseCard);
        return charts;
    }

    private VBox chartCard(String heading, javafx.scene.Node chart) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(18));
        card.setStyle(cardStyle());

        Label title = smallLabel(heading);
        VBox.setVgrow(chart, Priority.ALWAYS);
        card.getChildren().addAll(title, chart);

        return card;
    }

    private VBox buildInsightSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(20));
        section.setStyle(cardStyle());

        Label title = new Label("Financial Overview");
        title.setTextFill(Color.web(TEXT));
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 16));

        Label insight = new Label(
                "Select a date range to explore your business performance. " +
                "Your analytics are calculated from recorded accounting transactions."
        );
        insight.setWrapText(true);
        insight.setTextFill(Color.web(MUTED));
        insight.setFont(Font.font("Roboto", 12));

        section.getChildren().addAll(title, insight);
        return section;
    }

    private void refreshAnalytics() {
        if (startDatePicker == null || endDatePicker == null) {
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null || start.isAfter(end)) {
            showError("Please select a valid date range.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(
                DB_URL, DB_USER, DB_PASSWORD)) {

            Map<String, Double> totals = loadTotals(connection, start, end);

            double revenue = totals.getOrDefault("REVENUE", 0.0);
            double expenses = totals.getOrDefault("EXPENSE", 0.0);
            double netIncome = revenue - expenses;

            revenueValue.setText(currency(revenue));
            expenseValue.setText(currency(expenses));
            netIncomeValue.setText(currency(netIncome));

            double margin = revenue == 0 ? 0 : (netIncome / revenue) * 100;
            profitMarginValue.setText(String.format(Locale.US, "%.2f%%", margin));

            loadTrendChart(connection, start, end);
            loadExpenseChart(connection, start, end);

        } catch (SQLException ex) {
            showError("Unable to load analytics: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Map<String, Double> loadTotals(
            Connection connection, LocalDate start, LocalDate end
    ) throws SQLException {

        Map<String, Double> totals = new HashMap<>();

        String sql = """
                SELECT
                    UPPER(a.account_type) AS account_type,
                    SUM(j.debit - j.credit) AS balance
                FROM journal_entry_lines j
                JOIN chart_of_accounts a
                    ON j.account_id = a.account_id
                JOIN journal_entries e
                    ON j.entry_id = e.entry_id
                WHERE e.entry_date BETWEEN ? AND ?
                  AND UPPER(e.status) = 'POSTED'
                GROUP BY UPPER(a.account_type)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(start));
            statement.setDate(2, java.sql.Date.valueOf(end));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("account_type");
                    double balance = rs.getDouble("balance");

                    if (type != null) {
                        totals.put(type, balance);
                    }
                }
            }
        }

        return totals;
    }

    private void loadTrendChart(
            Connection connection, LocalDate start, LocalDate end
    ) throws SQLException {

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        String sql = """
                SELECT
                    DATE_FORMAT(e.entry_date, '%Y-%m') AS period,
                    UPPER(a.account_type) AS account_type,
                    SUM(j.debit - j.credit) AS amount
                FROM journal_entry_lines j
                JOIN chart_of_accounts a
                    ON j.account_id = a.account_id
                JOIN journal_entries e
                    ON j.entry_id = e.entry_id
                WHERE e.entry_date BETWEEN ? AND ?
                  AND UPPER(e.status) = 'POSTED'
                  AND UPPER(a.account_type) IN ('REVENUE', 'EXPENSE')
                GROUP BY period, UPPER(a.account_type)
                ORDER BY period
                """;

        Map<String, Map<String, Double>> monthly = new TreeMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(start));
            statement.setDate(2, java.sql.Date.valueOf(end));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String period = rs.getString("period");
                    String type = rs.getString("account_type");

                    monthly.computeIfAbsent(period, key -> new HashMap<>())
                            .put(type, rs.getDouble("amount"));
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yy");

        for (Map.Entry<String, Map<String, Double>> entry : monthly.entrySet()) {
            String label = YearMonth.parse(entry.getKey()).format(formatter);
            Map<String, Double> amounts = entry.getValue();

            revenueSeries.getData().add(new XYChart.Data<>(
                    label, Math.abs(amounts.getOrDefault("REVENUE", 0.0))
            ));

            expenseSeries.getData().add(new XYChart.Data<>(
                    label, amounts.getOrDefault("EXPENSE", 0.0)
            ));
        }

        trendChart.getData().setAll(revenueSeries, expenseSeries);
    }

    private void loadExpenseChart(
            Connection connection, LocalDate start, LocalDate end
    ) throws SQLException {

        expenseChart.getData().clear();

        String sql = """
                SELECT
                    a.account_name,
                    SUM(j.debit - j.credit) AS amount
                FROM journal_entry_lines j
                JOIN chart_of_accounts a
                    ON j.account_id = a.account_id
                JOIN journal_entries e
                    ON j.entry_id = e.entry_id
                WHERE e.entry_date BETWEEN ? AND ?
                  AND UPPER(e.status) = 'POSTED'
                  AND UPPER(a.account_type) = 'EXPENSE'
                GROUP BY a.account_name
                HAVING SUM(j.debit - j.credit) > 0
                ORDER BY amount DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(start));
            statement.setDate(2, java.sql.Date.valueOf(end));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    expenseChart.getData().add(
                            new PieChart.Data(
                                    rs.getString("account_name"),
                                    rs.getDouble("amount")
                            )
                    );
                }
            }
        }
    }

    private void setDefaultDates() {
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now);
    }

    private Label metricValue() {
        Label label = new Label("₱0.00");
        label.setTextFill(Color.web(TEXT));
        label.setFont(Font.font("Roboto", FontWeight.BOLD, 23));
        return label;
    }

    private Label smallLabel(String value) {
        Label label = new Label(value);
        label.setTextFill(Color.web(MUTED));
        label.setFont(Font.font("Roboto", FontWeight.BOLD, 10));
        return label;
    }

    private String currency(double amount) {
        return String.format(Locale.US, "₱%,.2f", amount);
    }

    private String cardStyle() {
        return "-fx-background-color: " + CARD + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;";
    }

    private String buttonStyle(String background, String foreground) {
        return "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + foreground + ";" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 10px;" +
                "-fx-padding: 11 15 11 15;" +
                "-fx-background-radius: 7;";
    }

    private void styleDatePicker(DatePicker picker) {
        picker.setPrefWidth(145);
        picker.setStyle(
                "-fx-background-color: #30231C;" +
                "-fx-control-inner-background: #30231C;" +
                "-fx-text-fill: " + TEXT + ";"
        );
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Financial Analytics");
        alert.setHeaderText("Unable to load analytics");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
