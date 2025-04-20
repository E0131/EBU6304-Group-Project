package com.group69.finance.service;

import com.group69.finance.model.Category;
import com.group69.finance.model.Source;
import com.group69.finance.model.Transaction;
import com.group69.finance.repository.FinanceDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    private final FinanceDataRepository repository;
    private final CategorizationService categorizationService;

    @Autowired
    public CsvImportService(FinanceDataRepository repository, CategorizationService categorizationService) {
        this.repository = repository;
        this.categorizationService = categorizationService;
    }

    public List<Transaction> importTransactionsFromCsv(String filePath) throws Exception {
        List<Transaction> importedTransactions = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip header row
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 5) {
                    try {
                        LocalDate date = LocalDate.parse(values[0].trim(), dateFormatter);
                        String description = values[1].trim();
                        double amount = Double.parseDouble(values[2].trim());
                        Category category = Category.valueOf(values[3].trim().toUpperCase());
                        Source source = Source.valueOf(values[4].trim().toUpperCase());

                        // If category is uncategorized, use AI for automatic classification
                        boolean aiSuggested = false;
                        if (category == Category.UNCATEGORIZED) {
                            Transaction tempTransaction = new Transaction(date, description, amount, Category.UNCATEGORIZED, source, false);
                            Category suggestedCategory = categorizationService.suggestCategory(tempTransaction);
                            if (suggestedCategory != null && suggestedCategory != Category.UNCATEGORIZED) {
                                category = suggestedCategory;
                                aiSuggested = true;
                            }
                        }

                        Transaction transaction = new Transaction(date, description, amount, category, source, aiSuggested);
                        importedTransactions.add(transaction);
                    } catch (Exception e) {
                        // Log error but continue processing other rows
                        System.err.println("Error parsing line: " + line + ", Error: " + e.getMessage());
                    }
                }
            }
        }

        // Add imported transactions to repository
        for (Transaction transaction : importedTransactions) {
            repository.addTransaction(transaction);
        }

        return importedTransactions;
    }
} 