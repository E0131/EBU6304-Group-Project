package com.group69.finance.controller;

import com.group69.finance.model.Category;
import com.group69.finance.model.Source;
import com.group69.finance.model.Transaction;
import com.group69.finance.repository.FinanceDataRepository;
import com.group69.finance.service.CategorizationService;
// Import PersistenceService if CSV import/export is added
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser; // For CSV import/export
import jakarta.annotation.PostConstruct; // Or javax.annotation if using older Spring Boot / different Jakarta EE setup
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; // Use @Component for FXML Controllers managed by Spring

import java.io.File; // For CSV import/export
import java.io.IOException; // For CSV import/export
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component // Mark as a Spring component
public class MainWindowController {

    private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

    // --- Injected Fields (from Spring Context) ---
    private final FinanceDataRepository repository;
    private final CategorizationService categorizationService;
    // Inject PersistenceService if needed for CSV:
    // private final PersistenceService persistenceService;

    // --- FXML Injected Fields (UI Elements) ---
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, String> descriptionCol;
    @FXML private TableColumn<Transaction, Double> amountCol;
    @FXML private TableColumn<Transaction, Category> categoryCol;
    @FXML private TableColumn<Transaction, Source> sourceCol;
    @FXML private TableColumn<Transaction, Boolean> aiCol;

    @FXML private DatePicker datePicker;
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private ComboBox<Source> sourceComboBox;

    @FXML private Button addButton;
    @FXML private Button clearButton; // Added clear button

    // Menu Items (can be handled directly or via injected MainFrame reference)
    @FXML private MenuItem loadMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem exitMenuItem;

    // Observable list to back the TableView
    private ObservableList<Transaction> transactionData = FXCollections.observableArrayList();

    // --- Constructor Injection ---
    @Autowired
    public MainWindowController(FinanceDataRepository repository, CategorizationService categorizationService) {
        this.repository = repository;
        this.categorizationService = categorizationService;
        log.info("MainWindowController initialized.");
        // PersistenceService injection if needed:
        // this.persistenceService = persistenceService;
    }

    // --- Initialization ---
    @PostConstruct // Good place for non-UI init logic, but UI init happens in initialize()
    public void postConstruct() {
        log.debug("MainWindowController PostConstruct called.");
        // Load initial data (happens in repository @PostConstruct)
        // Set up data list for the table
        transactionData.setAll(repository.getAllTransactions());
    }


    // Called by FXMLLoader after injecting FXML fields
    @FXML
    private void initialize() {
        log.debug("Initializing FXML components...");
        // --- Setup Table Columns ---
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        aiCol.setCellValueFactory(new PropertyValueFactory<>("aiSuggestedCategory"));

        // --- Custom Cell Rendering (Optional but recommended) ---
        // Format Date
        dateCol.setCellFactory(column -> new TableCell<Transaction, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(Transaction.DATE_FORMATTER));
                }
            }
        });

        // Format Amount (e.g., currency) - Basic version shown
        amountCol.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(""); // Reset style
                } else {
                    // Basic number formatting
                    setText(String.format("%.2f", item));
                    // Example: Color code amount (red for negative/expense, green for positive/income)
                    Category category = getTableView().getItems().get(getIndex()).getCategory();
                    if (category != null && category.isIncome()) {
                        setStyle("-fx-text-fill: green; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setStyle("-fx-text-fill: red; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });


        // Render Boolean as Y/N for AI column
        aiCol.setCellFactory(column -> new TableCell<Transaction, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item ? "Y" : "N"));
                setStyle("-fx-alignment: CENTER;");
            }
        });


        // --- Make Category Column Editable ---
        categoryCol.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(Category.values())));
        categoryCol.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            Category oldCategory = event.getOldValue();
            Category newCategory = event.getNewValue();
            if (transaction != null && newCategory != null && !newCategory.equals(oldCategory)) {
                log.debug("Manual category change via table edit: {} -> {}", oldCategory, newCategory);
                transaction.setCategory(newCategory); // This sets AI flag to false
                // Find the index in the underlying repository list (assuming table isn't sorted/filtered differently)
                int modelIndex = transactionData.indexOf(transaction); // May need adjustment if list != repo list directly
                if (modelIndex != -1) {
                    repository.updateTransaction(modelIndex, transaction); // Update repository
                    // Refresh the specific row's AI flag display if needed, or rely on full refresh
                    // transactionTable.refresh(); // Could refresh whole table
                    // Or update just the boolean cell for efficiency:
                    transactionData.set(modelIndex, transaction); // Trigger table update for the row
                } else {
                    log.warn("Could not find transaction index in observable list for update.");
                }
            }
        });


        // --- Populate ComboBoxes for Input Form ---
        categoryComboBox.setItems(FXCollections.observableArrayList(Category.values()));
        sourceComboBox.setItems(FXCollections.observableArrayList(Source.values()));

        // --- Set Default Values for Input Form ---
        handleClearForm(null); // Use clear form logic for defaults

        // --- Link TableView to ObservableList ---
        transactionTable.setItems(transactionData);
        log.debug("FXML components initialized.");

        // Initial data is loaded by repository, refresh view just in case
        refreshTableView();
    }

    // --- Event Handlers ---

    @FXML
    void handleAddTransaction(ActionEvent event) {
        try {
            LocalDate date = datePicker.getValue();
            String description = descriptionField.getText();
            String amountText = amountField.getText();

            // Basic Validation
            if (date == null) {
                showErrorDialog("Input Error", "Date cannot be empty.");
                return;
            }
            if (description == null || description.isBlank()) {
                showErrorDialog("Input Error", "Description cannot be empty.");
                return;
            }
            if (amountText == null || amountText.isBlank()) {
                showErrorDialog("Input Error", "Amount cannot be empty.");
                return;
            }

            double amount = Double.parseDouble(amountText); // Can throw NumberFormatException
            Category selectedCategory = categoryComboBox.getValue();
            Source source = sourceComboBox.getValue();

            if (selectedCategory == null) {
                showErrorDialog("Input Error", "Category must be selected.");
                return;
            }
            if (source == null) {
                showErrorDialog("Input Error", "Source must be selected.");
                return;
            }

            // --- AI Categorization Logic (if default selected) ---
            boolean aiSuggested = false;
            Category finalCategory = selectedCategory;
            // Create temporary transaction to pass to AI service (only if necessary)
            if (selectedCategory == Category.UNCATEGORIZED) {
                Transaction tempTransaction = new Transaction(date, description, amount, Category.UNCATEGORIZED, source, false);
                finalCategory = categorizationService.suggestCategory(tempTransaction);
                aiSuggested = (finalCategory != Category.UNCATEGORIZED);
            }

            // --- Create and Add Transaction ---
            Transaction newTransaction = new Transaction(date, description, amount, finalCategory, source, aiSuggested);
            repository.addTransaction(newTransaction);

            // --- Update UI ---
            // transactionData.add(newTransaction); // Add directly or refresh
            refreshTableView(); // Refreshing is simpler
            handleClearForm(null); // Clear form after adding

            log.info("Added transaction: {}", description);

        } catch (NumberFormatException ex) {
            log.warn("Amount parse error: {}", amountField.getText(), ex);
            showErrorDialog("Input Error", "Invalid amount. Please enter a valid number.");
        } catch (Exception ex) {
            log.error("Error adding transaction", ex);
            showErrorDialog("Error", "An unexpected error occurred: " + ex.getMessage());
        }
    }

    @FXML
    void handleDeleteTransaction(ActionEvent event) {
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();

        if (selectedTransaction == null) {
            showInfoDialog("Delete Transaction", "Please select a transaction to delete.");
            return;
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete the selected transaction?\n\n" + selectedTransaction.getDescription(),
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText(null); // No header

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Find index in the repository list
                // This assumes the observable list directly reflects the repo list order
                int modelIndex = transactionData.indexOf(selectedTransaction);
                if (modelIndex != -1) {
                    repository.removeTransactionAtIndex(modelIndex);
                    // transactionData.remove(selectedTransaction); // Remove directly or refresh
                    refreshTableView(); // Refreshing is simpler
                    log.info("Deleted transaction: {}", selectedTransaction.getDescription());
                } else {
                    log.warn("Could not find selected transaction in observable list for deletion.");
                    // Fallback: try removing by ID if IDs are reliable
                    // repository.removeTransactionById(selectedTransaction.getId());
                    refreshTableView();
                }
            }
        });
    }

    @FXML
    void handleClearForm(ActionEvent event) {
        datePicker.setValue(LocalDate.now());
        descriptionField.clear();
        amountField.clear();
        categoryComboBox.setValue(Category.UNCATEGORIZED); // Default to Uncategorized
        sourceComboBox.getSelectionModel().selectFirst(); // Select first source or null
        descriptionField.requestFocus(); // Set focus to description
    }

    @FXML
    void handleLoadData(ActionEvent event) {
        // Data is loaded initially by repository. This reloads from the default file.
        repository.loadInitialData();
        refreshTableView();
        showInfoDialog("Load Data", "Data reloaded successfully from default file.");
        log.info("Handled Load Data request.");
    }

    @FXML
    void handleSaveData(ActionEvent event) {
        repository.saveAllData(); // Repository knows the path
        showInfoDialog("Save Data", "Data saved successfully.");
        log.info("Handled Save Data request.");
    }

    @FXML
    void handleImportCsv(ActionEvent event) {
        // TODO: Implement CSV Import Logic
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Transactions from CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(transactionTable.getScene().getWindow()); // Get stage

        if (selectedFile != null) {
            showInfoDialog("Import CSV", "CSV Import functionality not yet implemented.\nSelected file: " + selectedFile.getName());
            // Steps:
            // 1. Create/Use a CsvPersistenceService implementing PersistenceService or a dedicated import service.
            // 2. Call the service to read transactions from selectedFile.
            // 3. Handle potential errors during CSV parsing (IOException, format errors).
            // 4. Add the successfully parsed transactions to the repository.
            // 5. Refresh the table view.
            // Example call:
             /*
             try {
                 // Assuming csvPersistenceService exists and implements loadTransactions
                 List<Transaction> importedTransactions = csvPersistenceService.loadTransactions(selectedFile.getAbsolutePath());
                 importedTransactions.forEach(repository::addTransaction); // Add to repository
                 refreshTableView();
                 showInfoDialog("Import CSV", "Successfully imported " + importedTransactions.size() + " transactions.");
             } catch (IOException e) {
                 log.error("Error importing CSV", e);
                 showErrorDialog("Import Error", "Failed to import CSV file: " + e.getMessage());
             }
             */
        }
    }


    @FXML
    void handleExit(ActionEvent event) {
        // Optionally confirm save before exiting
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save changes before exiting?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText(null);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                handleSaveData(null); // Save data
                Platform.exit(); // Exit JavaFX application
                System.exit(0); // Ensure JVM terminates if Platform.exit isn't enough
            } else if (response == ButtonType.NO) {
                Platform.exit();
                System.exit(0);
            }
            // If CANCEL, do nothing
        });
    }


    // --- Helper Methods ---

    private void refreshTableView() {
        log.debug("Refreshing TableView...");
        // Get fresh data from repository and update the observable list
        transactionData.setAll(repository.getAllTransactions());
        // No need to call transactionTable.setItems() again if it was set initially
        // transactionTable.sort(); // Re-apply sort if needed
        log.debug("TableView refreshed with {} items.", transactionData.size());
    }

    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}