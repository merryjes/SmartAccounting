package com.smartaccounting.ui;

import com.smartaccounting.dao.TrialBalanceDAO;
import com.smartaccounting.model.TrialBalanceRow;
import com.smartaccounting.db.DatabaseConnection;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;

public class DashboardPane {

    private static final String BG = "#17120F";
    private static final String SURFACE = "#211A16";
    private static final String SURFACE_2 = "#2B211B";
    private static final String BORDER = "#594438";
    private static final String TEXT = "#F4E9DC";
    private static final String MUTED = "#BBA795";
    private static final String ACCENT = "#C58B5C";
    private static final String GREEN = "#78977B";
    private static final String RED = "#B97869";

    private final Label assetsValue = new Label("—");
    private final Label liabilitiesValue = new Label("—");
    private final Label netIncomeValue = new Label("—");
    private final Label expensesValue = new Label("—");

    private final Label statusLabel = new Label("READY");
    private final Label lastUpdatedLabel = new Label("Not yet refreshed");

    private final ObservableList<ActivityRow> activities =
            FXCollections.observableArrayList();

    private BarChart<String, Number> positionChart;
    private BarChart<String, Number> incomeChart;
    private TableView<ActivityRow> activityTable;

    private BigDecimal totalAssets = BigDecimal.ZERO;
    private BigDecimal totalLiabilities = BigDecimal.ZERO;
    private BigDecimal totalEquity = BigDecimal.ZERO;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal netIncome = BigDecimal.ZERO;

    public BorderPane build(
            Runnable openJournal,
            Runnable openLedger,
            Runnable openIncomeStatement
    ) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        VBox page = new VBox(22);
        page.setPadding(new Insets(26));
        page.setFillWidth(true);

        page.getChildren().addAll(
                buildPageHeader(),
                buildSummaryCards(),
                buildCharts(),
                buildActivitySection(),
                buildQuickActions(openJournal, openLedger, openIncomeStatement)
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: " + BG + ";" +
                "-fx-background-color: " + BG + ";" +
                "-fx-border-color: transparent;"
        );

        root.setCenter(scroll);
        refreshData();

        return root;
    }

    // ---------------------------------------------------------
    // PAGE HEADER
    // ---------------------------------------------------------

    private Node buildPageHeader() {
        VBox header = new VBox(7);

        Label eyebrow = new Label("EXECUTIVE OVERVIEW");
        eyebrow.setStyle(
                "-fx-text-fill: " + ACCENT + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 2px;"
        );

        Label title = new Label("Dashboard");
        title.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 29px;" +
                "-fx-font-weight: bold;"
        );

        Label subtitle = new Label(
                "A clear view of your business finances and accounting activity."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        Button refresh = new Button("Refresh data");
        refresh.setStyle(buttonStyle(ACCENT));
        refresh.setOnAction(e -> refreshData());

        HBox bottom = new HBox(12, subtitle, new Region(), refresh);
        HBox.setHgrow(bottom.getChildren().get(1), Priority.ALWAYS);
        bottom.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(eyebrow, title, bottom);
        return header;
    }

    // ---------------------------------------------------------
    // SUMMARY CARDS
    // ---------------------------------------------------------

    private Node buildSummaryCards() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        grid.add(summaryCard("TOTAL ASSETS", assetsValue, ACCENT,
                "Resources owned by the business"), 0, 0);

        grid.add(summaryCard("TOTAL LIABILITIES", liabilitiesValue, RED,
                "Amounts owed by the business"), 1, 0);

        grid.add(summaryCard("NET INCOME", netIncomeValue, GREEN,
                "Revenue less expenses"), 2, 0);

        grid.add(summaryCard("TOTAL EXPENSES", expensesValue, ACCENT,
                "Posted business expenses"), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }

        return grid;
    }

    private VBox summaryCard(String heading, Label value, String accent,
                             String description) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setMinHeight(132);
        card.setStyle(cardStyle());

        Label title = new Label(heading);
        title.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        value.setStyle(
                "-fx-text-fill: " + accent + ";" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;"
        );

        Label note = new Label(description);
        note.setWrapText(true);
        note.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        card.getChildren().addAll(title, value, note);
        return card;
    }

    // ---------------------------------------------------------
    // CHARTS
    // ---------------------------------------------------------

    private Node buildCharts() {
        VBox positionCard = new VBox(14);
        positionCard.setPadding(new Insets(20));
        positionCard.setStyle(cardStyle());

        Label positionTitle = sectionTitle("FINANCIAL POSITION");
        Label positionSubtitle = mutedText(
                "Assets, liabilities, and equity including current earnings"
        );

        CategoryAxis positionXAxis = new CategoryAxis();
        positionXAxis.setLabel("Account category");

        NumberAxis positionYAxis = new NumberAxis();
        positionYAxis.setLabel("Amount");

        positionChart = new BarChart<>(positionXAxis, positionYAxis);
        positionChart.setLegendVisible(false);
        positionChart.setAnimated(false);
        positionChart.setCategoryGap(35);
        positionChart.setBarGap(8);
        positionChart.setPrefHeight(270);
        positionChart.setMinHeight(230);
        positionChart.setStyle(chartStyle());

        positionCard.getChildren().addAll(
                positionTitle, positionSubtitle, positionChart
        );

        VBox incomeCard = new VBox(14);
        incomeCard.setPadding(new Insets(20));
        incomeCard.setStyle(cardStyle());

        Label incomeTitle = sectionTitle("INCOME PERFORMANCE");
        Label incomeSubtitle = mutedText(
                "Comparison of posted revenue and expenses"
        );

        CategoryAxis incomeXAxis = new CategoryAxis();
        incomeXAxis.setLabel("Category");

        NumberAxis incomeYAxis = new NumberAxis();
        incomeYAxis.setLabel("Amount");

        incomeChart = new BarChart<>(incomeXAxis, incomeYAxis);
        incomeChart.setLegendVisible(false);
        incomeChart.setAnimated(false);
        incomeChart.setCategoryGap(45);
        incomeChart.setBarGap(10);
        incomeChart.setPrefHeight(270);
        incomeChart.setMinHeight(230);
        incomeChart.setStyle(chartStyle());

        incomeCard.getChildren().addAll(
                incomeTitle, incomeSubtitle, incomeChart
        );

        GridPane charts = new GridPane();
        charts.setHgap(15);
        charts.add(positionCard, 0, 0);
        charts.add(incomeCard, 1, 0);

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);

        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);

        charts.getColumnConstraints().addAll(left, right);

        return charts;
    }

    private void updateCharts() {
        XYChart.Series<String, Number> positionSeries =
                new XYChart.Series<>();

        positionSeries.getData().add(
                new XYChart.Data<>("Assets", totalAssets.doubleValue())
        );

        positionSeries.getData().add(
                new XYChart.Data<>("Liabilities", totalLiabilities.doubleValue())
        );

        positionSeries.getData().add(
                new XYChart.Data<>("Equity + earnings",
                        totalEquity.add(netIncome).doubleValue())
        );

        positionChart.getData().setAll(positionSeries);

        XYChart.Series<String, Number> incomeSeries =
                new XYChart.Series<>();

        incomeSeries.getData().add(
                new XYChart.Data<>("Revenue", totalRevenue.doubleValue())
        );

        incomeSeries.getData().add(
                new XYChart.Data<>("Expenses", totalExpenses.doubleValue())
        );

        incomeChart.getData().setAll(incomeSeries);
    }

    // ---------------------------------------------------------
    // RECENT ACCOUNTING ACTIVITY
    // ---------------------------------------------------------

    private Node buildActivitySection() {
        VBox section = new VBox(14);
        section.setPadding(new Insets(20));
        section.setStyle(cardStyle());

        Label title = sectionTitle("RECENT ACTIVITY");
        Label subtitle = mutedText(
                "Latest journal entries and adjusting entries"
        );

        activityTable = new TableView<>();
        activityTable.setItems(activities);
        activityTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        activityTable.setPlaceholder(
                new Label("No accounting activity found.")
        );
        activityTable.setPrefHeight(260);
        activityTable.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-control-inner-background: " + SURFACE + ";" +
                "-fx-table-cell-border-color: " + BORDER + ";" +
                "-fx-text-background-color: " + TEXT + ";"
        );

        TableColumn<ActivityRow, String> dateCol =
                new TableColumn<>("DATE");
        dateCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().date)
        );

        TableColumn<ActivityRow, String> referenceCol =
                new TableColumn<>("REFERENCE");
        referenceCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().reference)
        );

        TableColumn<ActivityRow, String> typeCol =
                new TableColumn<>("TYPE");
        typeCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().type)
        );

        TableColumn<ActivityRow, String> descriptionCol =
                new TableColumn<>("DESCRIPTION");
        descriptionCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().description)
        );

        TableColumn<ActivityRow, String> statusCol =
                new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().status)
        );

        dateCol.setPrefWidth(105);
        referenceCol.setPrefWidth(115);
        typeCol.setPrefWidth(110);
        descriptionCol.setPrefWidth(250);
        statusCol.setPrefWidth(100);

        activityTable.getColumns().addAll(
                dateCol, referenceCol, typeCol, descriptionCol, statusCol
        );

        section.getChildren().addAll(title, subtitle, activityTable);
        return section;
    }

    private void loadRecentActivity() throws Exception {
        activities.clear();

        String sql =
                "SELECT ActivityDate, ReferenceNo, Description, Status, ActivityType " +
                "FROM (" +
                " SELECT EntryDate AS ActivityDate, ReferenceNo, Description, " +
                " Status, 'JOURNAL' AS ActivityType " +
                " FROM JournalEntries " +
                " UNION ALL " +
                " SELECT AdjustmentDate AS ActivityDate, ReferenceNo, Description, " +
                " Status, 'ADJUSTMENT' AS ActivityType " +
                " FROM Adjustments " +
                ") AS RecentActivity " +
                "ORDER BY ActivityDate DESC LIMIT 8";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                String date = result.getDate("ActivityDate") == null
                        ? "—"
                        : result.getDate("ActivityDate").toString();

                activities.add(new ActivityRow(
                        date,
                        safe(result.getString("ReferenceNo")),
                        result.getString("ActivityType"),
                        safe(result.getString("Description")),
                        safe(result.getString("Status"))
                ));
            }
        }
    }

    // ---------------------------------------------------------
    // QUICK ACTIONS
    // ---------------------------------------------------------

    private Node buildQuickActions(
            Runnable openJournal,
            Runnable openLedger,
            Runnable openIncomeStatement
    ) {
        VBox section = new VBox(14);
        section.setPadding(new Insets(20));
        section.setStyle(cardStyle());

        Label title = sectionTitle("QUICK ACTIONS");
        Label subtitle = mutedText(
                "Jump directly to the tools you use most"
        );

        Button journalButton = actionButton(
                "NEW JOURNAL ENTRY", ACCENT, openJournal
        );

        Button ledgerButton = actionButton(
                "VIEW GENERAL LEDGER", SURFACE_2, openLedger
        );

        Button incomeButton = actionButton(
                "OPEN INCOME STATEMENT", SURFACE_2, openIncomeStatement
        );

        HBox buttons = new HBox(12,
                journalButton, ledgerButton, incomeButton
        );
        buttons.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(title, subtitle, buttons);
        return section;
    }

    private Button actionButton(String text, String color, Runnable action) {
        Button button = new Button(text);
        button.setStyle(buttonStyle(color));
        button.setOnAction(e -> action.run());
        return button;
    }

    // ---------------------------------------------------------
    // ACCOUNTING DATA
    // ---------------------------------------------------------

    private void refreshData() {
        statusLabel.setText("UPDATING...");
        statusLabel.setStyle("-fx-text-fill: " + ACCENT + ";");

        try {
            TrialBalanceDAO dao = new TrialBalanceDAO();

            TrialBalanceDAO.TrialBalanceSummary summary =
                    dao.getTrialBalance(true);

            totalAssets = BigDecimal.ZERO;
            totalLiabilities = BigDecimal.ZERO;
            totalEquity = BigDecimal.ZERO;
            totalRevenue = BigDecimal.ZERO;
            totalExpenses = BigDecimal.ZERO;

            for (TrialBalanceRow row : summary.getRows()) {
                BigDecimal debit = zeroIfNull(row.getDebitBalance());
                BigDecimal credit = zeroIfNull(row.getCreditBalance());

                String type = row.getAccountType();

                if (type == null) {
                    continue;
                }

                switch (type.trim().toUpperCase(Locale.ROOT)) {
                    case "ASSET":
                        totalAssets = totalAssets.add(debit.subtract(credit));
                        break;

                    case "LIABILITY":
                        totalLiabilities =
                                totalLiabilities.add(credit.subtract(debit));
                        break;

                    case "EQUITY":
                        totalEquity = totalEquity.add(credit.subtract(debit));
                        break;

                    case "REVENUE":
                        totalRevenue = totalRevenue.add(credit.subtract(debit));
                        break;

                    case "EXPENSE":
                        totalExpenses = totalExpenses.add(debit.subtract(credit));
                        break;

                    default:
                        break;
                }
            }

            netIncome = totalRevenue.subtract(totalExpenses);

            assetsValue.setText(money(totalAssets));
            liabilitiesValue.setText(money(totalLiabilities));
            netIncomeValue.setText(money(netIncome));
            expensesValue.setText(money(totalExpenses));

            updateCharts();
            loadRecentActivity();

            statusLabel.setText("DATA UPDATED");
            statusLabel.setStyle("-fx-text-fill: " + GREEN + ";");
            lastUpdatedLabel.setText("Latest figures loaded from your database.");

        } catch (Exception ex) {
            statusLabel.setText("UPDATE FAILED");
            statusLabel.setStyle("-fx-text-fill: " + RED + ";");
            lastUpdatedLabel.setText(
                    "Could not load accounting data: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    // ---------------------------------------------------------
    // DESIGN HELPERS
    // ---------------------------------------------------------

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );
        return label;
    }

    private Label mutedText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );
        return label;
    }

    private String cardStyle() {
        return
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-background-radius: 15;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 15;" +
                "-fx-border-width: 1;";
    }

    private String chartStyle() {
        return
                "-fx-background-color: transparent;" +
                "-fx-legend-visible: false;";
    }

    private String buttonStyle(String background) {
        return
                "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 11 16 11 16;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(
                Locale.forLanguageTag("en-PH")
        );
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);

        return "₱ " + format.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    // ---------------------------------------------------------
    // ACTIVITY ROW
    // ---------------------------------------------------------

    private static class ActivityRow {
        private final String date;
        private final String reference;
        private final String type;
        private final String description;
        private final String status;

        ActivityRow(
                String date,
                String reference,
                String type,
                String description,
                String status
        ) {
            this.date = date;
            this.reference = reference;
            this.type = type;
            this.description = description;
            this.status = status;
        }
    }
}
