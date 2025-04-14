package com.group69.finance.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private String id;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String description;
    private double amount; // Positive: income, Negative: expense
    private Category category;
    private Source source;
    private boolean aiSuggestedCategory;

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // No-arg constructor for Jackson/Frameworks
    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.date = LocalDate.now();
        this.category = Category.UNCATEGORIZED;
        this.source = Source.OTHER;
    }

    // Main constructor used in code
    public Transaction(LocalDate date, String description, double amount, Category category, Source source, boolean aiSuggested) {
        this(); // Set defaults like ID
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null").trim();
        this.amount = amount;
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.source = Objects.requireNonNull(source, "Source cannot be null");
        this.aiSuggestedCategory = aiSuggested;

        // Basic validation/adjustment based on amount sign and category type
        if (amount >= 0 && !this.category.isIncome() && this.category != Category.UNCATEGORIZED) {
            System.err.printf("Warning: Positive amount (%.2f) assigned to expense category '%s'. Check logic.%n", amount, category);
            // Optionally default to OTHER_INCOME or UNCATEGORIZED
            // this.category = Category.OTHER_INCOME;
        } else if (amount < 0 && this.category.isIncome() && this.category != Category.UNCATEGORIZED) {
            System.err.printf("Warning: Negative amount (%.2f) assigned to income category '%s'. Check logic.%n", amount, category);
            // Optionally default to OTHER_EXPENSE or UNCATEGORIZED
            // this.category = Category.OTHER_EXPENSE;
        }
    }

    // --- Getters ---
    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public Category getCategory() { return category; }
    public Source getSource() { return source; }
    public boolean isAiSuggestedCategory() { return aiSuggestedCategory; }

    // --- Setters (needed by Jackson if using no-arg constructor, also for modification) ---
    // Avoid public setId unless necessary
    public void setDate(LocalDate date) { this.date = Objects.requireNonNull(date); }
    public void setDescription(String description) { this.description = Objects.requireNonNull(description).trim(); }
    public void setAmount(double amount) { this.amount = amount; }
    public void setSource(Source source) { this.source = Objects.requireNonNull(source); }
    public void setAiSuggestedCategory(boolean aiSuggestedCategory) { this.aiSuggestedCategory = aiSuggestedCategory; }

    // Special setter for category resets AI flag
    public void setCategory(Category category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.aiSuggestedCategory = false; // Manual set overrides AI suggestion
    }

    @Override
    public String toString() {
        return String.format("Transaction{id='%s', date=%s, desc='%s', amount=%.2f, cat=%s, src=%s, ai=%b}",
                id, date.format(DATE_FORMATTER), description, amount, category, source, aiSuggestedCategory);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id); // ID is the unique identifier
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}