package com.group69.finance.controller;

import com.group69.finance.model.Category;
import com.group69.finance.model.Source;
import com.group69.finance.model.Transaction;
import com.group69.finance.repository.FinanceDataRepository;
import com.group69.finance.service.CategorizationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List; // Import List

@Component
public class MainWindowController {

    private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

    private final FinanceDataRepository repository;
    private final CategorizationService categorizationService;
    // private final PersistenceService persistenceService; // If needed for CSV

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
    @FXML private Button clearButton;

    @FXML private MenuItem loadMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem exitMenuItem;

    private ObservableList<Transaction> transactionData = FXCollections.observableArrayList();

    @Autowired
    public MainWindowController(FinanceDataRepository repository, CategorizationService categorizationService) {
        this.repository = repository;
        this.categorizationService = categorizationService;
        log.info("MainWindowController initialized.");
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("MainWindowController PostConstruct called.");
        transactionData.setAll(repository.getAllTransactions());
    }

    @FXML
    private void initialize() {
        log.debug("Initializing FXML components and setting cell factories...");

        // --- Setup Table Columns (Standard PropertyValueFactory) ---
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        aiCol.setCellValueFactory(new PropertyValueFactory<>("aiSuggestedCategory"));

        // --- Custom Cell Rendering (Apply CSS Classes) ---

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

        // Format Amount (Add alignment and conditional income/expense classes)
        amountCol.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().removeAll("amount-income", "amount-expense", "cell-align-right");

                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    getStyleClass().add("cell-align-right");

                    Transaction transaction = (Transaction) getTableRow().getItem();

                    if (transaction != null) {
                        Category category = transaction.getCategory();
                        if (category != null && category.isIncome()) {
                            getStyleClass().add("amount-income");
                        } else {
                            getStyleClass().add("amount-expense");
                        }
                    }
                }
            }
        });


        // Render Boolean as Y/N for AI column (Add alignment class)
        aiCol.setCellFactory(column -> new TableCell<Transaction, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("cell-align-center");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Y" : "N");
                    getStyleClass().add("cell-align-center");
                }
            }
        });


        // Make Category Column Editable
        categoryCol.setCellFactory( tc -> {
            ComboBoxTableCell<Transaction, Category> cell = new ComboBoxTableCell<>(FXCollections.observableArrayList(Category.values()));
            return cell;
        });
        categoryCol.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue(); // Get the data item for the row being edited
            Category oldCategory = event.getOldValue();
            Category newCategory = event.getNewValue();

            // Check if a valid change was made
            if (transaction != null && newCategory != null && !newCategory.equals(oldCategory)) {
                log.debug("Manual category change via table edit: {} -> {}", oldCategory, newCategory);
                transaction.setCategory(newCategory); // Update the model object (also sets AI flag to false in setter)

                int modelIndex = transactionData.indexOf(transaction); // Find index in our observable list
                if (modelIndex != -1) {
                    repository.updateTransaction(modelIndex, transaction); // Update the repository data

                    // Refresh the item in the ObservableList to ensure UI updates,
                    // especially for the AI? column which depends on the transaction state.
                    transactionData.set(modelIndex, transaction);

                    log.debug("Transaction updated in repository and UI list at index {}", modelIndex);
                } else {
                    log.warn("Could not find transaction index in observable list for update after edit commit.");
                    // Fallback: Refresh the whole table if index is lost
                    transactionTable.refresh();
                }
            } else if (transaction != null) {
                // This block handles cases where the edit was cancelled (e.g., Esc pressed)
                // or the new value was the same as the old one or null.
                // We need to ensure the cell visually reflects the *original* state of the transaction.
                log.debug("Category edit cancelled or no change occurred. Reverting visual state for row {}.", event.getTablePosition().getRow());

                // *** CORRECTED LINE ***
                // Use the event object to get the TableView reference.
                // Resetting the item in the list forces the TableView to redraw the row cells
                // using the current (unchanged) state of the transaction object.
                int viewIndex = event.getTablePosition().getRow();
                event.getTableView().getItems().set(viewIndex, transaction);

                // Optional: A full table refresh might be needed in rare cases if the above doesn't work
                // event.getTableView().refresh();
            } else {
                log.warn("Transaction object was null during edit commit handling for category.");
                // Refresh table as a precaution
                transactionTable.refresh();
            }
        });

        // Source column cell factory
        sourceCol.setCellFactory(column -> new TableCell<Transaction, Source>() {
            @Override
            protected void updateItem(Source item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });


        // --- Populate ComboBoxes for Input Form ---
        categoryComboBox.setItems(FXCollections.observableArrayList(Category.values()));
        sourceComboBox.setItems(FXCollections.observableArrayList(Source.values()));

        // --- Set Default Values for Input Form ---
        handleClearForm(null);

        // --- Link TableView to ObservableList ---
        transactionTable.setItems(transactionData);
        log.debug("FXML components initialized and cell factories configured.");

        // Initial data is loaded by repository
    }

    // --- Event Handlers ---
    // [ ... Keep ALL existing Event Handlers (handleAddTransaction, handleDeleteTransaction, etc.) ... ]
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

            double amount = Double.parseDouble(amountText);
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

            boolean aiSuggested = false;
            Category finalCategory = selectedCategory;
            if (selectedCategory == Category.UNCATEGORIZED) {
                Transaction tempTransaction = new Transaction(date, description, amount, Category.UNCATEGORIZED, source, false);
                finalCategory = categorizationService.suggestCategory(tempTransaction);
                aiSuggested = (finalCategory != Category.UNCATEGORIZED && finalCategory != null); // Ensure AI didn't return null
                if (finalCategory == null) finalCategory = Category.UNCATEGORIZED; // Fallback if AI fails
            }

            Transaction newTransaction = new Transaction(date, description, amount, finalCategory, source, aiSuggested);
            repository.addTransaction(newTransaction);

            refreshTableView(); // Refreshing updates the ObservableList which TableView observes
            handleClearForm(null);

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
                int modelIndex = transactionData.indexOf(selectedTransaction);
                if (modelIndex != -1) {
                    repository.removeTransactionAtIndex(modelIndex);
                    refreshTableView(); // Refresh observable list from repo
                    log.info("Deleted transaction: {}", selectedTransaction.getDescription());
                } else {
                    log.warn("Could not find selected transaction in observable list for deletion.");
                    refreshTableView(); // Refresh anyway
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
        descriptionField.requestFocus();
    }

    @FXML
    void handleLoadData(ActionEvent event) {
        repository.loadInitialData();
        refreshTableView();
        showInfoDialog("Load Data", "Data reloaded successfully from default file.");
        log.info("Handled Load Data request.");
    }

    @FXML
    void handleSaveData(ActionEvent event) {
        repository.saveAllData();
        showInfoDialog("Save Data", "Data saved successfully.");
        log.info("Handled Save Data request.");
    }

    @FXML
    void handleImportCsv(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Transactions from CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(transactionTable.getScene().getWindow()); // Get stage

        if (selectedFile != null) {
            log.warn("CSV Import functionality not yet implemented. Selected file: {}", selectedFile.getAbsolutePath());
            showInfoDialog("Import CSV", "CSV Import functionality not yet implemented.\nSelected file: " + selectedFile.getName());
        }
    }

    @FXML
    void handleAbout(ActionEvent event) {
        showInfoDialog("About", "Personal Finance Tracker v1.0 (Group69 FX)\nStyled with CSS");
    }


    @FXML
    void handleExit(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save changes before exiting?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText(null);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                handleSaveData(null);
                Platform.exit();
                System.exit(0);
            } else if (response == ButtonType.NO) {
                Platform.exit();
                System.exit(0);
            }
            // If CANCEL, do nothing
        });
    }

    // --- Helper Methods ---

    private void refreshTableView() {
        log.debug("Refreshing TableView data...");
        transactionData.setAll(repository.getAllTransactions());
        log.debug("TableView data refreshed with {} items.", transactionData.size());
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