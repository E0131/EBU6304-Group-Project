package com.group69.finance.service;

import com.group69.finance.model.Transaction;
import java.io.IOException;
import java.util.List;

// Interface for saving and loading transaction data
public interface PersistenceService {
    void saveTransactions(List<Transaction> transactions, String filePath) throws IOException;
    List<Transaction> loadTransactions(String filePath) throws IOException;
}