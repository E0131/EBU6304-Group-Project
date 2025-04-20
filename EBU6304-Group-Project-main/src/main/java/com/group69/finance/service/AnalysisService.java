package com.group69.finance.service;

import com.group69.finance.model.Transaction;
import com.group69.finance.repository.FinanceDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private final FinanceDataRepository repository;

    @Autowired
    public AnalysisService(FinanceDataRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> getAIAnalysis() {
        List<Transaction> transactions = repository.getAllTransactions();
        Map<String, Object> analysis = new HashMap<>();

        // 1. Expense trend analysis
        analysis.put("expenseTrend", analyzeExpenseTrend(transactions));

        // 2. Spending habits analysis
        analysis.put("spendingHabits", analyzeSpendingHabits(transactions));

        // 3. Budget advice
        analysis.put("budgetAdvice", generateBudgetAdvice(transactions));

        // 4. Anomaly detection
        analysis.put("anomalies", detectAnomalies(transactions));

        return analysis;
    }

    private Map<String, Object> analyzeExpenseTrend(List<Transaction> transactions) {
        Map<String, Object> trend = new HashMap<>();
        
        // Calculate monthly expenses
        Map<String, Double> monthlyExpenses = transactions.stream()
            .filter(t -> !t.getCategory().isIncome())
            .collect(Collectors.groupingBy(
                t -> t.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.summingDouble(Transaction::getAmount)
            ));

        // Calculate trend
        List<Double> values = new ArrayList<>(monthlyExpenses.values());
        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double lastMonth = values.isEmpty() ? 0 : values.get(values.size() - 1);
        double trendPercentage = average == 0 ? 0 : ((lastMonth - average) / average) * 100;

        trend.put("monthlyExpenses", monthlyExpenses);
        trend.put("averageExpense", average);
        trend.put("trendPercentage", trendPercentage);
        trend.put("trendDirection", trendPercentage > 0 ? "Increasing" : "Decreasing");

        return trend;
    }

    private Map<String, Object> analyzeSpendingHabits(List<Transaction> transactions) {
        Map<String, Object> habits = new HashMap<>();
        
        // Calculate expenses by category
        Map<String, Double> categoryExpenses = transactions.stream()
            .filter(t -> !t.getCategory().isIncome())
            .collect(Collectors.groupingBy(
                t -> t.getCategory().name(),
                Collectors.summingDouble(Transaction::getAmount)
            ));

        // Find main spending category
        String mainCategory = categoryExpenses.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("No data");

        habits.put("categoryExpenses", categoryExpenses);
        habits.put("mainCategory", mainCategory);
        habits.put("mainCategoryPercentage", calculatePercentage(categoryExpenses, mainCategory));

        return habits;
    }

    private Map<String, Object> generateBudgetAdvice(List<Transaction> transactions) {
        Map<String, Object> advice = new HashMap<>();
        
        // Calculate average monthly income and expenses
        double avgMonthlyIncome = transactions.stream()
            .filter(t -> t.getCategory().isIncome())
            .collect(Collectors.groupingBy(
                t -> t.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.summingDouble(Transaction::getAmount)
            ))
            .values()
            .stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0);

        double avgMonthlyExpense = transactions.stream()
            .filter(t -> !t.getCategory().isIncome())
            .collect(Collectors.groupingBy(
                t -> t.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.summingDouble(Transaction::getAmount)
            ))
            .values()
            .stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0);

        // Generate budget advice
        double savingsRate = (avgMonthlyIncome - avgMonthlyExpense) / avgMonthlyIncome * 100;
        String savingsAdvice = savingsRate < 20 ? "Consider increasing savings rate" : "Good savings rate";

        advice.put("avgMonthlyIncome", avgMonthlyIncome);
        advice.put("avgMonthlyExpense", avgMonthlyExpense);
        advice.put("savingsRate", savingsRate);
        advice.put("savingsAdvice", savingsAdvice);

        return advice;
    }

    private List<Map<String, Object>> detectAnomalies(List<Transaction> transactions) {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        
        // Calculate expenses by category
        Map<String, Double> categoryAverages = transactions.stream()
            .filter(t -> !t.getCategory().isIncome())
            .collect(Collectors.groupingBy(
                t -> t.getCategory().name(),
                Collectors.averagingDouble(Transaction::getAmount)
            ));

        // Detect anomalous expenses
        transactions.stream()
            .filter(t -> !t.getCategory().isIncome())
            .forEach(t -> {
                double avg = categoryAverages.getOrDefault(t.getCategory().name(), 0.0);
                if (t.getAmount() > avg * 3) { // 超过平均值的3倍视为异常
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("date", t.getDate());
                    anomaly.put("category", t.getCategory().name());
                    anomaly.put("amount", t.getAmount());
                    anomaly.put("description", t.getDescription());
                    anomaly.put("deviation", (t.getAmount() - avg) / avg * 100);
                    anomalies.add(anomaly);
                }
            });

        return anomalies;
    }

    private double calculatePercentage(Map<String, Double> expenses, String category) {
        double total = expenses.values().stream().mapToDouble(Double::doubleValue).sum();
        return total == 0 ? 0 : (expenses.getOrDefault(category, 0.0) / total) * 100;
    }
} 