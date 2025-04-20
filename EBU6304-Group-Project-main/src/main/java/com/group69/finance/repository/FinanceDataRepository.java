package com.group69.finance.repository;

import com.group69.finance.model.Transaction;
import com.group69.finance.service.PersistenceService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList; // Thread-safe list

@Repository
public class FinanceDataRepository {

    private static final Logger log = LoggerFactory.getLogger(FinanceDataRepository.class);

    // Use a thread-safe list for internal storage
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final PersistenceService persistenceService;
    private final String dataFilePath;

    @Autowired
    public FinanceDataRepository(
            // Use @Qualifier if multiple PersistenceService beans exist, otherwise optional
            // @Qualifier("jsonPersistenceService") PersistenceService persistenceService,
            PersistenceService persistenceService, // Spring finds the JsonPersistenceService bean
            @Value("${app.data.filepath}") String dataFilePath) {
        this.persistenceService = persistenceService;
        this.dataFilePath = dataFilePath;
        log.info("FinanceDataRepository initialized. Data file path: {}", dataFilePath);
    }

    @PostConstruct
    public void loadInitialData() {
        log.info("Attempting to load initial data from: {}", dataFilePath);
        try {
            List<Transaction> loaded = persistenceService.loadTransactions(dataFilePath);
            // Use setAll for CopyOnWriteArrayList or clear/addAll
            transactions.clear();
            transactions.addAll(loaded);
            log.info("Successfully loaded {} transactions.", loaded.size());
        } catch (IOException e) {
            log.warn("Could not load initial data from {}. Starting with empty list. Error: {}", dataFilePath, e.getMessage());
            transactions.clear();
        } catch (Exception e) { // Catch unexpected errors during load
            log.error("Unexpected error loading initial data from {}", dataFilePath, e);
            transactions.clear();
        }
    }

    public void saveAllData() {
        log.info("Attempting to save {} transactions to: {}", transactions.size(), dataFilePath);
        try {
            // Pass a stable copy (ArrayList) to the persistence service
            persistenceService.saveTransactions(new ArrayList<>(transactions), dataFilePath);
            log.info("Successfully saved data.");
        } catch (IOException e) {
            log.error("Failed to save data to {}: {}", dataFilePath, e.getMessage());
            // Consider notifying user through Controller/UI
        } catch (Exception e) {
            log.error("Unexpected error saving data to {}", dataFilePath, e);
        }
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transactions.add(transaction);
            log.debug("Added transaction: {}", transaction.getId());
            // saveAllData(); // Optional: Save immediately after adding
        }
    }

    public boolean removeTransactionAtIndex(int index) {
        if (index >= 0 && index < transactions.size()) {
            Transaction removed = this.transactions.remove(index);
            log.debug("Removed transaction at index {}: {}", index, removed.getId());
            // saveAllData(); // Optional: Save immediately
            return true;
        } else {
            log.warn("Attempted to remove transaction at invalid index: {}", index);
            return false;
        }
    }

    public boolean removeTransactionById(String id) {
        boolean removed = this.transactions.removeIf(t -> t.getId().equals(id));
        if (removed) {
            log.debug("Removed transaction by ID: {}", id);
            // saveAllData(); // Optional: Save immediately
        } else {
            log.warn("Attempted to remove transaction with non-existent ID: {}", id);
        }
        return removed;
    }


    public Optional<Transaction> getTransaction(int index) {
        if (index >= 0 && index < transactions.size()) {
            return Optional.of(transactions.get(index));
        }
        return Optional.empty();
    }

    public Optional<Transaction> getTransactionById(String id) {
        return this.transactions.stream().filter(t -> t.getId().equals(id)).findFirst();
    }


    public List<Transaction> getAllTransactions() {
        // Return an immutable list view to prevent external modification
        return Collections.unmodifiableList(transactions);
    }

    public int getSize() {
        return transactions.size();
    }

    public boolean updateTransaction(int index, Transaction updatedTransaction) {
        if (updatedTransaction != null && index >= 0 && index < transactions.size()) {
            // Ensure ID consistency if needed, or simply replace
            Transaction oldTransaction = transactions.get(index);
            if (!oldTransaction.getId().equals(updatedTransaction.getId())){
                log.warn("Updating transaction at index {} but ID mismatch! old={}, new={}", index, oldTransaction.getId(), updatedTransaction.getId());
                // Decide how to handle ID mismatch - ignore, throw error, copy old ID?
                // For simplicity, we replace but log:
            }
            this.transactions.set(index, updatedTransaction);
            log.debug("Updated transaction at index {}: {}", index, updatedTransaction.getId());
            // saveAllData(); // Optional: Save immediately
            return true;
        } else {
            log.warn("Attempted to update transaction at invalid index {} or with null data.", index);
            return false;
        }
    }
    // Optional: Update by ID
    public boolean updateTransactionById(String id, Transaction updatedTransaction) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(id)) {
                transactions.set(i, updatedTransaction);
                log.debug("Updated transaction by ID: {}", id);
                // saveAllData(); // Optional: Save immediately
                return true;
            }
        }
        log.warn("Attempted to update transaction with non-existent ID: {}", id);
        return false;
    }

}