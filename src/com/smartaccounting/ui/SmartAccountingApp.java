package com.smartaccounting.ui;
import com.smartaccounting.dao.*;
import com.smartaccounting.db.DatabaseConnection;
import com.smartaccounting.model.*;
import com.smartaccounting.service.IncomeStatementService;
import com.smartaccounting.service.TransactionValidator;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;


/**
* Smart Accounting — Dark Mokka Edition
* Replacement UI for the existing SmartAccounting project.
* Keeps the project's existing DAO, model, database and service classes.
*/
public class SmartAccountingApp extends Application  {
    private static final String BG = "#17120F";
    private static final String SURFACE = "#211A16";
    private static final String SURFACE_2 = "#2B211B";
    private static final String BORDER = "#594438";
    private static final String TEXT = "#F4E9DC";
    private static final String MUTED = "#BBA795";
    private static final String ACCENT = "#C58B5C";
    private static final String ACCENT_2 = "#8F684D";
    private static final String GREEN = "#78977B";
    private static final String RED = "#B97869";
    private static final String FONT = "'Roboto', 'Inter', 'DejaVu Sans'";
    private static final String PIXEL_FONT = "'Roboto', 'Inter', 'DejaVu Sans'";
    private final ObservableList<AccountOption> chartAccounts = FXCollections.observableArrayList();
    private final ObservableList<LineInputRow> lineRows = FXCollections.observableArrayList();
    private Label totalDebitLabel = new Label("DEBIT  0.00");
    private Label totalCreditLabel = new Label("CREDIT  0.00");
    private Label balanceStatusLabel = new Label("WAITING FOR JOURNAL LINES");
    private Integer editingUnpostedEntryId;
    private DatePicker entryDatePicker;
    private TextField entryRefField, entryDescField;
    private CheckBox entryPostBox;
    private Label entryFeedbackLabel;
    private VBox linesContainer;
    private TabPane mainTabPane;
    private TableView<String[]> coaTable;
    private static final class AccountOption  {
        final int accountId;
        final String accountCode, accountName, accountType, normalBalance;
        AccountOption(int id, String code, String name, String type, String normal)  {
            accountId=id;
            accountCode=code;
            accountName=name;
            accountType=type;
            normalBalance=normal;
        }
        @Override public String toString()  {
            return accountCode + " · " + accountName + " (" + accountType + ")";
        }
    }
    private static final class LineInputRow  {
        final ComboBox<AccountOption> accountCombo = new ComboBox<>();
        final TextField debitField = new TextField("0.00"), creditField = new TextField("0.00");
        final HBox box = new HBox(9);
        LineInputRow(List<AccountOption> accounts, Runnable changed, Runnable remove)  {
            accountCombo.setItems(FXCollections.observableArrayList(accounts));
            accountCombo.setPromptText("Choose account");
            accountCombo.setPrefWidth(270);
            styleCombo(accountCombo);
            debitField.setPrefWidth(115);
            creditField.setPrefWidth(115);
            debitField.setPromptText("0.00");
            creditField.setPromptText("0.00");
            styleField(debitField);
            styleField(creditField);
            Button removeButton = button("Remove", RED);
            removeButton.setOnAction(e -> remove.run());
            accountCombo.valueProperty().addListener((o,a,b)->changed.run());
            debitField.textProperty().addListener((o,a,b)->changed.run());
            creditField.textProperty().addListener((o,a,b)->changed.run());
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(label("ACCOUNT"), accountCombo, label("DR"), debitField, label("CR"), creditField, removeButton);
        }
    }
    @Override public void start(Stage stage)  {
        stage.setTitle("Smart Accounting · Dark Mokka");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        showWelcome(stage);
    }
    private void showWelcome(Stage stage)  {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        VBox panel = new VBox(22);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(760);
        panel.setPadding(new Insets(54));
        panel.setStyle(glassStyle());
        Label eyebrow = new Label("YOUR FINANCES, IN BALANCE");
        eyebrow.setStyle(pixelStyle(16, ACCENT));
        Label title = new Label("SMART ACCOUNTING");
        title.setWrapText(true);
        title.setStyle(pixelStyle(42, TEXT));
        Label sub = new Label("A calm little workspace for journals, ledgers and financial reports.");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);
        sub.setStyle(textStyle(14, MUTED));
        Label status = new Label("INITIALIZING YOUR WORKSPACE...");
        status.setStyle(pixelStyle(13, ACCENT_2));
        Button enter = button("OPEN WORKSPACE", ACCENT);
        enter.setDefaultButton(true);
        enter.setOnAction(e ->  {
            reloadChartOfAccounts(); showDashboard(stage);
        });
        panel.getChildren().addAll(eyebrow,title,sub,status,enter);
        root.getChildren().add(panel);
        FadeTransition fade = new FadeTransition(Duration.seconds(1.7), panel);
        fade.setFromValue(0.12);
        fade.setToValue(1);
        fade.play();
        FadeTransition pulse = new FadeTransition(Duration.seconds(1.5), eyebrow);
        pulse.setFromValue(0.35);
        pulse.setToValue(1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(FadeTransition.INDEFINITE);
        pulse.play();
        Scene scene = new Scene(root, 1220, 790);
        installCss(scene);
        stage.setScene(scene);
        stage.show();
    }
    private void showDashboard(Stage stage)  {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        VBox header = new VBox(5);
        header.setPadding(new Insets(20,26,18,26));
        header.setStyle("-fx-background-color: " + SURFACE + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        Label title = new Label("SMART ACCOUNTING");
        title.setStyle(pixelStyle(25,TEXT));
        Label subtitle = new Label("DOUBLE-ENTRY BOOKKEEPING  ·  ADJUSTMENTS  ·  FINANCIAL REPORTS");
        subtitle.setStyle(textStyle(10,MUTED));
        header.getChildren().addAll(title,subtitle);
        root.setTop(header);
        reloadChartOfAccounts();
        TableView<String[]> journalTable = buildTable("ENTRY ID","DATE","REFERENCE","STATUS","ACCOUNT","DESCRIPTION","DEBIT","CREDIT");
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabPane.getTabs().addAll( new Tab("01  ACCOUNTS", buildChartOfAccountsPane()), new Tab("02  JOURNAL ENTRY", buildJournalEntryPane(journalTable)), new Tab("03  GENERAL JOURNAL", buildGeneralJournalPane(stage,journalTable)), new Tab("04  LEDGER", buildGeneralLedgerPane(stage)), new Tab("05  TRIAL BALANCE", buildTrialBalancePane(stage)), new Tab("06  ADJUSTMENTS", buildAdjustingEntriesPane()), new Tab("07  INCOME STATEMENT", buildIncomeStatementPane(stage)));
        root.setCenter(mainTabPane);
        VBox footer = new VBox(new Label("DARK MOKKA EDITION   •   YOUR RECORDS STAY IN YOUR DATABASE"));
        footer.setPadding(new Insets(9,20,9,20));
        footer.setStyle("-fx-background-color: " + SURFACE + ";");
        footer.getChildren().get(0).setStyle(textStyle(9,MUTED));
        root.setBottom(footer);
        Scene scene = new Scene(root,1220,790);
        installCss(scene);
        stage.setScene(scene);
        FadeTransition f = new FadeTransition(Duration.millis(450),root);
        f.setFromValue(0);
        f.setToValue(1);
        f.play();
    }
    private void installCss(Scene scene)  {
        scene.getRoot().setStyle("-fx-font-family: " + FONT + "; -fx-background-color: " + BG + ";");
        try  {
            var cssUrl = getClass().getResource("/styles/mokka.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored)  {
        }
    }
    private void reloadChartOfAccounts()  {
        chartAccounts.clear();
        String sql="SELECT AccountID, AccountCode, AccountName, AccountType, NormalBalance FROM Accounts ORDER BY AccountCode, AccountID";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery())  {
            while(r.next()) chartAccounts.add(new AccountOption(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));
        } catch(Exception ex)  {
            System.err.println("Could not load accounts: "+ex.getMessage());
        }
        if(coaTable!=null) refreshCoaTable("");
        for(LineInputRow row:lineRows)  {
            AccountOption selected=row.accountCombo.getValue();
            row.accountCombo.setItems(FXCollections.observableArrayList(chartAccounts));
            if(selected!=null) for(AccountOption a:chartAccounts) if(a.accountId==selected.accountId)  {
                row.accountCombo.setValue(a);
                break;
            }
        }
    }
    private VBox buildChartOfAccountsPane()  {
        VBox root=pane();
        VBox card=card();
        Label heading=heading("ADD AN ACCOUNT");
        TextField code=new TextField(), name=new TextField();
        code.setPromptText("Account code");
        name.setPromptText("Account name");
        styleField(code);
        styleField(name);
        ComboBox<String> type=new ComboBox<>(FXCollections.observableArrayList("Assets","Liabilities","Owner's Equity","Revenue","Expenses"));
        type.setValue("Assets");
        styleCombo(type);
        ComboBox<String> normal=new ComboBox<>(FXCollections.observableArrayList("Debit","Credit"));
        normal.setValue("Debit");
        styleCombo(normal);
        type.valueProperty().addListener((o,a,b)->normal.setValue(("Assets".equals(b)||"Expenses".equals(b))?"Debit":"Credit"));
        Label msg=message();
        Button add=button("ADD ACCOUNT",ACCENT);
        add.setOnAction(e-> {
            if(code.getText().isBlank()||name.getText().isBlank()) {
                msg.setText("Enter both an account code and name.");return;
            }
            String dbType=switch(type.getValue()) {
                case "Assets"->"ASSET";case "Liabilities"->"LIABILITY";case "Owner's Equity"->"EQUITY";case "Revenue"->"REVENUE";case "Expenses"->"EXPENSE";default->throw new IllegalArgumentException("Invalid account type");
            }; try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO Accounts (AccountCode,AccountName,AccountType,NormalBalance) VALUES (?,?,?,?)")) {
                p.setString(1,code.getText().trim());p.setString(2,name.getText().trim());p.setString(3,dbType);p.setString(4,normal.getValue());p.executeUpdate();msg.setText("Account added successfully.");code.clear();name.clear();reloadChartOfAccounts();
            } catch(Exception ex) {
                msg.setText("Could not add account: "+ex.getMessage());
            }
        });
        HBox form=new HBox(10,code,name,type,normal,add);
        form.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(heading,form,msg);
        TextField filter=new TextField();
        filter.setPromptText("Filter accounts...");
        styleField(filter);
        Button refresh=button("FILTER / REFRESH",ACCENT_2);
        refresh.setOnAction(e->refreshCoaTable(filter.getText()));
        coaTable=buildTable("ID","CODE","ACCOUNT NAME","CLASSIFICATION","NORMAL BALANCE");
        root.getChildren().addAll(card,new HBox(10,filter,refresh),coaTable);
        VBox.setVgrow(coaTable,Priority.ALWAYS);
        refreshCoaTable("");
        return root;
    }
    private void refreshCoaTable(String q) {
        if(coaTable==null)return;
        ObservableList<String[]> rows=FXCollections.observableArrayList();
        String query=q==null?"":q.toLowerCase().trim();
        for(AccountOption a:chartAccounts) {
            String[] r= {
                ""+a.accountId,a.accountCode,a.accountName,a.accountType,a.normalBalance
            };
            if(matches(r,query))rows.add(r);
        }
        coaTable.setItems(rows);
    }
    
private VBox buildJournalEntryPane(TableView<String[]> journalTable) {
    VBox root = pane();
    VBox c = card();

    // Initialize the journal metadata fields
    entryDatePicker = new DatePicker(LocalDate.now());

    entryRefField = new TextField();
    entryRefField.setPromptText("JV-001");

    entryDescField = new TextField();
    entryDescField.setPromptText("Describe the transaction");

    styleField(entryRefField);
    styleField(entryDescField);

    HBox.setHgrow(entryDescField, Priority.ALWAYS);

    HBox meta = new HBox(
        10,
        label("DATE"),
        entryDatePicker,
        label("REFERENCE"),
        entryRefField,
        label("DESCRIPTION"),
        entryDescField
    );

    meta.setAlignment(Pos.CENTER_LEFT);

    // Initialize journal line container
    linesContainer = new VBox(9);

    // Initialize totals and feedback labels
    totalDebitLabel = new Label("DEBIT  0.00");
    totalCreditLabel = new Label("CREDIT  0.00");
    balanceStatusLabel = new Label("ENTER AMOUNTS");

    totalDebitLabel.setStyle(textStyle(12, TEXT));
    totalCreditLabel.setStyle(textStyle(12, TEXT));
    balanceStatusLabel.setStyle(pixelStyle(13, ACCENT));

    HBox totals = new HBox(
        24,
        totalDebitLabel,
        totalCreditLabel,
        balanceStatusLabel
    );

    totals.setPadding(new Insets(10, 0, 10, 0));

    // Initialize posting checkbox BEFORE resetting the form
    entryPostBox = new CheckBox("POST IMMEDIATELY");
    entryPostBox.setSelected(true);

    entryFeedbackLabel = message();

    // Initialize buttons
    Button add = button("+ ADD LINE", GREEN);
    add.setOnAction(e -> addNewLine());

    Button reload = button("RELOAD ACCOUNTS", ACCENT_2);
    reload.setOnAction(e -> reloadChartOfAccounts());

    HBox tools = new HBox(9, add, reload);

    Button save = button("SAVE JOURNAL ENTRY", ACCENT);
    save.setOnAction(e -> saveJournal(journalTable));

    Button clear = button("CLEAR FORM", ACCENT_2);
    clear.setOnAction(e -> {
        editingUnpostedEntryId = null;
        resetEntryForm();
        entryFeedbackLabel.setText("Form cleared.");
    });

    // Assemble the interface FIRST
    c.getChildren().addAll(
        heading("JOURNAL ENTRY WORKSPACE"),
        meta,
        new Separator(),
        tools,
        linesContainer,
        new Separator(),
        totals,
        new HBox(12, entryPostBox, save, clear),
        entryFeedbackLabel
    );

    root.getChildren().add(c);

    // Reset only after all controls are initialized
    resetEntryForm();

    return root;
}

    private void saveJournal(TableView<String[]> table) {
        try {
            if(entryRefField.getText().isBlank()) {
                entryFeedbackLabel.setText("Please enter a reference number.");
                return;
            }
            JournalEntry entry=new JournalEntry(entryDatePicker.getValue(),entryRefField.getText().trim(),entryDescField.getText().trim());
            for(LineInputRow r:lineRows) {
                AccountOption a=r.accountCombo.getValue();
                if(a==null) {
                    entryFeedbackLabel.setText("Choose an account for every line.");
                    return;
                }
                JournalLine l=new JournalLine();
                l.setAccountId(a.accountId);
                l.setDebit(parseAmount(r.debitField.getText()));
                l.setCredit(parseAmount(r.creditField.getText()));
                entry.addLine(l);
            }
            List<String> errors=TransactionValidator.validate(entry);
            if(!errors.isEmpty()) {
                entryFeedbackLabel.setText(String.join(" | ",errors));
                return;
            }
            if(editingUnpostedEntryId!=null) {
                updateUnpostedEntry(editingUnpostedEntryId,entry,entryPostBox.isSelected());
                entryFeedbackLabel.setText("Updated unposted entry #"+editingUnpostedEntryId);
                editingUnpostedEntryId=null;
            } else  {
                int id=new JournalDAO().saveJournalEntry(entry,entryPostBox.isSelected());
                entryFeedbackLabel.setText("Saved entry #"+id+" · "+entry.getStatus());
            }
            resetEntryForm();
            refreshJournalTable(table,"");
        } catch(Exception ex) {
            entryFeedbackLabel.setText("Save failed: "+ex.getMessage());
        }
    }
    private void resetEntryForm() {
        if(entryDatePicker==null)return;
        entryDatePicker.setValue(LocalDate.now());
        entryRefField.clear();
        entryDescField.clear();
        entryPostBox.setSelected(true);
        linesContainer.getChildren().clear();
        lineRows.clear();
        addNewLine();
        addNewLine();
        recalculateTotals();
    }
    private void addNewLine() {
        final LineInputRow[] holder=new LineInputRow[1];
        LineInputRow row=new LineInputRow(chartAccounts,this::recalculateTotals,()-> {
            if(lineRows.size()>2) {
                lineRows.remove(holder[0]);linesContainer.getChildren().remove(holder[0].box);recalculateTotals();
            }
        });
        holder[0]=row;
        lineRows.add(row);
        linesContainer.getChildren().add(row.box);
        recalculateTotals();
    }
    private void recalculateTotals() {
        BigDecimal dr=BigDecimal.ZERO,cr=BigDecimal.ZERO;
        for(LineInputRow r:lineRows) {
            dr=dr.add(parseAmount(r.debitField.getText()));
            cr=cr.add(parseAmount(r.creditField.getText()));
        }
        totalDebitLabel.setText("DEBIT  "+dr.toPlainString());
        totalCreditLabel.setText("CREDIT  "+cr.toPlainString());
        balanceStatusLabel.setText(dr.signum()==0&&cr.signum()==0?"ENTER AMOUNTS":dr.compareTo(cr)==0?"BALANCED":"UNBALANCED  "+dr.subtract(cr).abs());
    }
    private VBox buildGeneralJournalPane(Stage stage,TableView<String[]> table) {
        VBox root=pane();
        TextField search=new TextField();
        search.setPromptText("Search journal...");
        styleField(search);
        Label msg=message();
        Button find=button("SEARCH / REFRESH",ACCENT_2);
        find.setOnAction(e->refreshJournalTable(table,search.getText()));
        Button post=button("POST SELECTED",GREEN);
        post.setOnAction(e-> {
            String[] s=table.getSelectionModel().getSelectedItem();if(s==null) {
                msg.setText("Select a journal row first.");return;
            }
            try {
                int id=Integer.parseInt(s[0]);msg.setText(new JournalDAO().postJournalEntry(id)?"Entry #"+id+" posted.":"Entry is already posted or missing.");refreshJournalTable(table,search.getText());
            } catch(Exception ex) {
                msg.setText(ex.getMessage());
            }
        });
        Button edit=button("EDIT UNPOSTED",ACCENT_2);
        edit.setOnAction(e-> {
            String[] s=table.getSelectionModel().getSelectedItem();if(s==null) {
                msg.setText("Select an unposted journal row.");return;
            }
            loadEntryForEditing(Integer.parseInt(s[0]),msg);
        });
        Button delete=button("DELETE UNPOSTED",RED);
        delete.setOnAction(e-> {
            String[] s=table.getSelectionModel().getSelectedItem();if(s==null) {
                msg.setText("Select an unposted journal row.");return;
            }
            try {
                deleteUnpostedEntry(Integer.parseInt(s[0]));msg.setText("Unposted entry deleted.");refreshJournalTable(table,search.getText());
            } catch(Exception ex) {
                msg.setText("Delete blocked: "+ex.getMessage());
            }
        });
        Button export=button("EXPORT CSV",ACCENT);
        export.setOnAction(e->exportCsv(stage,table,"general_journal.csv",msg));
        root.getChildren().addAll(new HBox(9,search,find,post,edit,delete,export),table,msg);
        VBox.setVgrow(table,Priority.ALWAYS);
        refreshJournalTable(table,"");
        return root;
    }
    private void loadEntryForEditing(int entryId,Label msg) {
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT EntryDate,ReferenceNo,Description,Status FROM JournalEntries WHERE EntryID=?")) {
            p.setInt(1,entryId);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) {
                    msg.setText("Entry not found.");
                    return;
                }
                if(!"UNPOSTED".equalsIgnoreCase(r.getString("Status"))) {
                    msg.setText("Only UNPOSTED entries can be edited.");
                    return;
                }
                editingUnpostedEntryId=entryId;
                entryDatePicker.setValue(r.getDate("EntryDate").toLocalDate());
                entryRefField.setText(r.getString("ReferenceNo"));
                entryDescField.setText(r.getString("Description"));
            }
            linesContainer.getChildren().clear();
            lineRows.clear();
            try(PreparedStatement lp=c.prepareStatement("SELECT AccountID,Debit,Credit FROM JournalLines WHERE EntryID=? ORDER BY LineID")) {
                lp.setInt(1,entryId);
                try(ResultSet lr=lp.executeQuery()) {
                    while(lr.next()) {
                        addNewLine();
                        LineInputRow row=lineRows.get(lineRows.size()-1);
                        for(AccountOption a:chartAccounts)if(a.accountId==lr.getInt("AccountID")) {
                            row.accountCombo.setValue(a);
                            break;
                        }
                        row.debitField.setText(lr.getBigDecimal("Debit").toPlainString());
                        row.creditField.setText(lr.getBigDecimal("Credit").toPlainString());
                    }
                }
            }
            recalculateTotals();
            entryFeedbackLabel.setText("Editing UNPOSTED entry #"+entryId);
            mainTabPane.getSelectionModel().select(1);
        } catch(Exception ex) {
            msg.setText("Could not load entry: "+ex.getMessage());
        }
    }
    private void deleteUnpostedEntry(int entryId)throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement check=c.prepareStatement("SELECT Status FROM JournalEntries WHERE EntryID=?")) {
                check.setInt(1,entryId);
                try(ResultSet r=check.executeQuery()) {
                    if(!r.next())throw new SQLException("Entry not found.");
                    if(!"UNPOSTED".equalsIgnoreCase(r.getString("Status")))throw new SQLException("Posted entries cannot be deleted.");
                }
            }
            try(PreparedStatement lines=c.prepareStatement("DELETE FROM JournalLines WHERE EntryID=?");PreparedStatement entry=c.prepareStatement("DELETE FROM JournalEntries WHERE EntryID=? AND Status='UNPOSTED'")) {
                lines.setInt(1,entryId);
                lines.executeUpdate();
                entry.setInt(1,entryId);
                entry.executeUpdate();
                c.commit();
            } catch(Exception ex) {
                c.rollback();
                throw ex instanceof SQLException?(SQLException)ex:new SQLException(ex);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
    private void refreshJournalTable(TableView<String[]> table,String q) {
        ObservableList<String[]> rows=FXCollections.observableArrayList();
        try {
            for(JournalEntry je:new GeneralJournalDAO().getGeneralJournal(false))for(JournalLine l:je.getLines()) {
                String acct=(l.getAccountCode()==null?"":l.getAccountCode()+" · ")+(l.getAccountName()==null?"Account "+l.getAccountId():l.getAccountName());
                String[] r= {
                    ""+je.getEntryId(),je.getEntryDate().toString(),je.getReferenceNo(),je.getStatus(),acct,je.getDescription(),l.getDebit().toPlainString(),l.getCredit().toPlainString()
                };
                if(matches(r,q==null?"":q.toLowerCase().trim()))rows.add(r);
            }
        } catch(Exception ex) {
            System.err.println("Journal load: "+ex.getMessage());
        }
        table.setItems(rows);
    }
    private VBox buildGeneralLedgerPane(Stage stage) {
        VBox root=pane();
        ComboBox<AccountOption> acct=new ComboBox<>(chartAccounts);
        acct.setPromptText("Select account");
        acct.setPrefWidth(280);
        styleCombo(acct);
        if(!chartAccounts.isEmpty())acct.setValue(chartAccounts.get(0));
        CheckBox adjusted=new CheckBox("Include posted adjusting entries");
        TableView<String[]> table=buildTable("DATE","SOURCE","REFERENCE","DESCRIPTION","DEBIT","CREDIT","RUNNING BALANCE");
        Label msg=message();
        Button load=button("LOAD LEDGER",ACCENT);
        load.setOnAction(e-> {
            if(acct.getValue()==null) {
                msg.setText("Select an account.");return;
            }
            try {
                List<LedgerEntryRow> data=new GeneralLedgerDAO().getLedgerForAccount(acct.getValue().accountId,adjusted.isSelected());ObservableList<String[]> rows=FXCollections.observableArrayList();for(LedgerEntryRow r:data)rows.add(new String[] {
                    r.getDate().toString(),r.getSourceType(),r.getReferenceNo(),r.getDescription(),r.getDebit().toPlainString(),r.getCredit().toPlainString(),r.getRunningBalance().toPlainString()
                });table.setItems(rows);msg.setText("Loaded "+rows.size()+" ledger rows.");
            } catch(Exception ex) {
                msg.setText(ex.getMessage());
            }
        });
        Button export=button("EXPORT CSV",ACCENT_2);
        export.setOnAction(e->exportCsv(stage,table,"general_ledger.csv",msg));
        root.getChildren().addAll(new HBox(10,label("ACCOUNT"),acct,adjusted,load,export),table,msg);
        VBox.setVgrow(table,Priority.ALWAYS);
        return root;
    }
    private VBox buildTrialBalancePane(Stage stage) {
        VBox root=pane();
        TableView<String[]> table=buildTable("ACCOUNT CODE","ACCOUNT NAME","TYPE","DEBIT","CREDIT");
        Label summary=message();
        Button unadj=button("UNADJUSTED",ACCENT_2),adj=button("ADJUSTED",GREEN),export=button("EXPORT CSV",ACCENT);
        unadj.setOnAction(e->loadTrial(false,"UNADJUSTED",table,summary));
        adj.setOnAction(e->loadTrial(true,"ADJUSTED",table,summary));
        export.setOnAction(e->exportCsv(stage,table,"trial_balance.csv",summary));
        root.getChildren().addAll(new HBox(10,unadj,adj,export),table,summary);
        VBox.setVgrow(table,Priority.ALWAYS);
        loadTrial(false,"UNADJUSTED",table,summary);
        return root;
    }
    private void loadTrial(boolean includeAdj,String name,TableView<String[]> table,Label msg) {
        try {
            TrialBalanceDAO.TrialBalanceSummary s=new TrialBalanceDAO().getTrialBalance(includeAdj);
            ObservableList<String[]> rows=FXCollections.observableArrayList();
            for(TrialBalanceRow r:s.getRows())rows.add(new String[] {
                r.getAccountCode(),r.getAccountName(),r.getAccountType(),r.getDebitBalance().toPlainString(),r.getCreditBalance().toPlainString()
            });
            table.setItems(rows);
            msg.setText(name+"  ·  DR "+s.getTotalDebit()+"  ·  CR "+s.getTotalCredit()+"  ·  DIFF "+s.getDifference()+"  ·  "+(s.isBalanced()?"BALANCED":"NOT BALANCED"));
        } catch(Exception ex) {
            msg.setText("Trial balance error: "+ex.getMessage());
        }
    }
    private VBox buildAdjustingEntriesPane() {
        VBox root=pane();
        VBox c=card();
        DatePicker date=new DatePicker(LocalDate.now());
        TextField ref=new TextField();
        ref.setPromptText("ADJ-001");
        ComboBox<AccountOption> dr=new ComboBox<>(chartAccounts),cr=new ComboBox<>(chartAccounts);
        dr.setPromptText("Debit account");
        cr.setPromptText("Credit account");
        styleCombo(dr);
        styleCombo(cr);
        TextField amount=new TextField();
        amount.setPromptText("Amount");
        TextField reason=new TextField();
        reason.setPromptText("Reason / explanation");
        CheckBox post=new CheckBox("Post immediately");
        post.setSelected(true);
        Label msg=message();
        Button save=button("SAVE ADJUSTMENT",ACCENT);
        TableView<String[]> table=buildTable("ID","DATE","REFERENCE","DEBIT ACCT","CREDIT ACCT","AMOUNT","STATUS","REASON");
        save.setOnAction(e-> {
            try {
                if(dr.getValue()==null||cr.getValue()==null)throw new IllegalArgumentException("Choose both accounts.");AdjustmentEntry a=new AdjustmentEntry();a.setAdjustmentDate(date.getValue());a.setReferenceNo(ref.getText().trim());a.setDebitAccountId(dr.getValue().accountId);a.setCreditAccountId(cr.getValue().accountId);a.setAmount(new BigDecimal(amount.getText().trim()));a.setDescription(reason.getText().trim());int id=new AdjustmentDAO().saveAdjustment(a,post.isSelected());msg.setText("Saved adjustment #"+id+". Original journal entries remain unchanged.");ref.clear();amount.clear();reason.clear();refreshAdjustments(table);
            } catch(Exception ex) {
                msg.setText("Adjustment error: "+ex.getMessage());
            }
        });
        c.getChildren().addAll(heading("CREATE ADJUSTING ENTRY"),new HBox(9,date,ref,dr,cr,amount),new HBox(9,reason,post,save),msg);
        root.getChildren().addAll(c,table);
        VBox.setVgrow(table,Priority.ALWAYS);
        refreshAdjustments(table);
        return root;
    }
    private void refreshAdjustments(TableView<String[]> table) {
        ObservableList<String[]> rows=FXCollections.observableArrayList();
        String sql="SELECT a.AdjustmentID,a.AdjustmentDate,a.ReferenceNo,a.Description,a.Status,dl.AccountID DrAcct,cl.AccountID CrAcct,dl.Debit Amt FROM Adjustments a LEFT JOIN AdjustmentLines dl ON a.AdjustmentID=dl.AdjustmentID AND dl.Debit>0 LEFT JOIN AdjustmentLines cl ON a.AdjustmentID=cl.AdjustmentID AND cl.Credit>0 ORDER BY a.AdjustmentDate,a.AdjustmentID";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()) {
            while(r.next()) {
                BigDecimal a=r.getBigDecimal("Amt");
                rows.add(new String[] {
                    ""+r.getInt("AdjustmentID"),r.getDate("AdjustmentDate").toString(),r.getString("ReferenceNo"),""+r.getInt("DrAcct"),""+r.getInt("CrAcct"),a==null?"0.00":a.toPlainString(),r.getString("Status"),r.getString("Description")
                });
            }
        } catch(Exception ex) {
            System.err.println("Adjustments: "+ex.getMessage());
        }
        table.setItems(rows);
    }
    private VBox buildIncomeStatementPane(Stage stage) {
        VBox root=pane();
        TextField business=new TextField("My Business"),period=new TextField("For the period ended November 2026");
        styleField(business);
        styleField(period);
        TableView<String[]> table=buildTable("SECTION","ACCOUNT","AMOUNT");
        Label net=message();
        Button generate=button("GENERATE STATEMENT",ACCENT);
        generate.setOnAction(e-> {
            try {
                var report=new IncomeStatementService().generateAdjustedIncomeStatement(business.getText().trim(),period.getText().trim());ObservableList<String[]> rows=FXCollections.observableArrayList();for(Map.Entry<String,BigDecimal>x:report.getRevenues().entrySet())rows.add(new String[] {
                    "REVENUE",x.getKey(),x.getValue().toPlainString()
                });rows.add(new String[] {
                    "TOTAL REVENUE","",report.getTotalRevenue().toPlainString()
                });for(Map.Entry<String,BigDecimal>x:report.getExpenses().entrySet())rows.add(new String[] {
                    "EXPENSE",x.getKey(),x.getValue().toPlainString()
                });rows.add(new String[] {
                    "TOTAL EXPENSES","",report.getTotalExpenses().toPlainString()
                });table.setItems(rows);net.setText(report.getBusinessName()+"  ·  "+report.getAccountingPeriod()+"  ·  "+(report.isNetIncome()?"NET INCOME ":"NET LOSS ")+report.getNetAmount().abs());
            } catch(Exception ex) {
                net.setText("Statement error: "+ex.getMessage());
            }
        });
        Button export=button("EXPORT CSV",ACCENT_2);
        export.setOnAction(e->exportCsv(stage,table,"income_statement.csv",net));
        root.getChildren().addAll(new HBox(9,label("BUSINESS"),business,label("PERIOD"),period,generate,export),table,net);
        VBox.setVgrow(table,Priority.ALWAYS);
        return root;
    }
    private void updateUnpostedEntry(int id,JournalEntry entry,boolean postNow)throws SQLException {
        String up="UPDATE JournalEntries SET EntryDate=?,ReferenceNo=?,Description=?,Status=? WHERE EntryID=? AND Status='UNPOSTED'";
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement(up);PreparedStatement d=c.prepareStatement("DELETE FROM JournalLines WHERE EntryID=?");PreparedStatement i=c.prepareStatement("INSERT INTO JournalLines (EntryID,AccountID,Debit,Credit) VALUES (?,?,?,?)")) {
                p.setDate(1,java.sql.Date.valueOf(entry.getEntryDate()));
                p.setString(2,entry.getReferenceNo());
                p.setString(3,entry.getDescription());
                p.setString(4,postNow?"POSTED":"UNPOSTED");
                p.setInt(5,id);
                if(p.executeUpdate()==0)throw new SQLException("Entry is not unposted or does not exist.");
                d.setInt(1,id);
                d.executeUpdate();
                for(JournalLine l:entry.getLines()) {
                    i.setInt(1,id);
                    i.setInt(2,l.getAccountId());
                    i.setBigDecimal(3,l.getDebit());
                    i.setBigDecimal(4,l.getCredit());
                    i.addBatch();
                }
                i.executeBatch();
                c.commit();
            } catch(Exception ex) {
                c.rollback();
                throw ex instanceof SQLException?(SQLException)ex:new SQLException(ex);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
    private void exportCsv(Stage stage,TableView<String[]> table,String defaultName,Label msg) {
        FileChooser fc=new FileChooser();
        fc.setTitle("Export report");
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
        File f=fc.showSaveDialog(stage);
        if(f==null)return;
        try(PrintWriter out=new PrintWriter(f,StandardCharsets.UTF_8)) {
            List<String> heads=new ArrayList<>();
            for(TableColumn<String[],?> c:table.getColumns())heads.add(csv(c.getText()));
            out.println(String.join(",",heads));
            for(String[] row:table.getItems()) {
                List<String> cells=new ArrayList<>();
                for(String v:row)cells.add(csv(v));
                out.println(String.join(",",cells));
            }
            msg.setText("CSV exported to "+f.getAbsolutePath());
        } catch(Exception ex) {
            msg.setText("Export failed: "+ex.getMessage());
        }
    }
    private static String csv(String s) {
        return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";
    }
    private TableView<String[]> buildTable(String... headers) {
        TableView<String[]> table = new TableView<>();

        table.getStyleClass().add("mokka-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No records available"));

        for (int i = 0; i < headers.length; i++) {
            final int index = i;

            TableColumn<String[], String> column =
                    new TableColumn<>(headers[i]);

            column.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue()[index]));

            column.setStyle(
                    "-fx-alignment: CENTER-LEFT;" +
                    "-fx-text-fill: #EAD8C5;"
            );

            table.getColumns().add(column);
        }

        return table;
    }


    private static boolean matches(String[] row,String q) {
        if(q==null||q.isBlank())return true;
        for(String s:row)if(s!=null&&s.toLowerCase().contains(q))return true;
        return false;
    }
    private static BigDecimal parseAmount(String s) {
        try {
            return s==null||s.isBlank()?BigDecimal.ZERO:new BigDecimal(s.trim());
        } catch(Exception e) {
            return BigDecimal.ZERO;
        }
    }
    private static VBox pane() {
        VBox v=new VBox(14);
        v.setPadding(new Insets(20));
        return v;
    }
    private static VBox card() {
        VBox v=new VBox(13);
        v.setPadding(new Insets(19));
        v.setStyle(glassStyle());
        return v;
    }
    private static String glassStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, #30241D, #211A16); -fx-background-radius: 16; -fx-border-color: #594438; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 18, 0.18, 0, 6);";
    }
    private static Label heading(String s) {
        Label l=new Label(s);
        l.setStyle(pixelStyle(17,ACCENT));
        return l;
    }
    private static Label label(String s) {
        Label l=new Label(s);
        l.setStyle(textStyle(10,MUTED));
        return l;
    }
    private static Label message() {
        Label l=new Label();
        l.setWrapText(true);
        l.setStyle(textStyle(11,TEXT));
        return l;
    }
    private static Button button(String s,String color) {
        Button b=new Button(s);
        b.setStyle("-fx-background-color: "+color+"; -fx-text-fill: #17120F; -fx-font-family: "+FONT+"; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 9; -fx-padding: 10 15 10 15; -fx-cursor: hand;");
        b.setOnMouseEntered(e->b.setOpacity(.82));
        b.setOnMouseExited(e->b.setOpacity(1));
        return b;
    }
    private static void styleField(TextField f) {
        f.setStyle("-fx-background-color: #30241D; -fx-text-fill: "+TEXT+"; -fx-prompt-text-fill: #9F8B7B; -fx-border-color: "+BORDER+"; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 9;");
    }
    private static void styleCombo(ComboBox<?> c) {
        c.setStyle("-fx-background-color: #30241D; -fx-border-color: "+BORDER+"; -fx-border-radius: 8; -fx-background-radius: 8;");
    }
    private static String textStyle(int size,String color) {
        return "-fx-font-family: "+FONT+"; -fx-font-size: "+size+"px; -fx-text-fill: "+color+";";
    }
    private static String pixelStyle(int size,String color) {
        return "-fx-font-family: "+PIXEL_FONT+"; -fx-font-size: "+size+"px; -fx-text-fill: "+color+"; -fx-font-weight: bold;";
    }
    public static void main(String[] args) {
        launch(args);
    }
}
