package com.group69.finance.controller;

import com.group69.finance.model.Transaction;
import com.group69.finance.repository.FinanceDataRepository;
import com.group69.finance.service.AnalysisService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class AnalysisViewController {

    @FXML
    private PieChart categoryPieChart;
    
    @FXML
    private BarChart<String, Number> monthlyBarChart;
    
    @FXML
    private Text expenseTrendText;
    
    @FXML
    private Text spendingHabitsText;
    
    @FXML
    private Text budgetAdviceText;
    
    @FXML
    private Text anomaliesText;
    
    private final FinanceDataRepository repository;
    private final ApplicationContext springContext;
    private final AnalysisService analysisService;
    
    @Autowired
    public AnalysisViewController(FinanceDataRepository repository, 
                                ApplicationContext springContext,
                                AnalysisService analysisService) {
        this.repository = repository;
        this.springContext = springContext;
        this.analysisService = analysisService;
    }
    
    @FXML
    public void initialize() {
        updateCharts();
        updateAIAnalysis();
    }
    
    private void updateCharts() {
        // Update pie chart
        updateCategoryPieChart();
        
        // Update bar chart
        updateMonthlyBarChart();
    }
    
    private void updateAIAnalysis() {
        Map<String, Object> analysis = analysisService.getAIAnalysis();
        
        // Update expense trend analysis
        Map<String, Object> expenseTrend = (Map<String, Object>) analysis.get("expenseTrend");
        String trendText = String.format("Average Monthly Expense: %.2f\n" +
                                       "Recent Expense Trend: %s (%.1f%%)",
                (Double) expenseTrend.get("averageExpense"),
                expenseTrend.get("trendDirection"),
                (Double) expenseTrend.get("trendPercentage"));
        expenseTrendText.setText(trendText);
        
        // Update spending habits analysis
        Map<String, Object> spendingHabits = (Map<String, Object>) analysis.get("spendingHabits");
        String habitsText = String.format("Main Spending Category: %s (%.1f%%)",
                spendingHabits.get("mainCategory"),
                (Double) spendingHabits.get("mainCategoryPercentage"));
        spendingHabitsText.setText(habitsText);
        
        // Update budget advice
        Map<String, Object> budgetAdvice = (Map<String, Object>) analysis.get("budgetAdvice");
        String adviceText = String.format("Average Monthly Income: %.2f\n" +
                                        "Average Monthly Expense: %.2f\n" +
                                        "Savings Rate: %.1f%%\n" +
                                        "Advice: %s",
                (Double) budgetAdvice.get("avgMonthlyIncome"),
                (Double) budgetAdvice.get("avgMonthlyExpense"),
                (Double) budgetAdvice.get("savingsRate"),
                budgetAdvice.get("savingsAdvice"));
        budgetAdviceText.setText(adviceText);
        
        // Update anomaly detection
        List<Map<String, Object>> anomalies = (List<Map<String, Object>>) analysis.get("anomalies");
        StringBuilder anomaliesBuilder = new StringBuilder();
        if (anomalies.isEmpty()) {
            anomaliesBuilder.append("No anomalous expenses detected");
        } else {
            anomaliesBuilder.append("Detected anomalous expenses:\n");
            for (Map<String, Object> anomaly : anomalies) {
                anomaliesBuilder.append(String.format("- %s: %s (%.2f, deviated by %.1f%%)\n",
                        anomaly.get("date"),
                        anomaly.get("description"),
                        anomaly.get("amount"),
                        anomaly.get("deviation")));
            }
        }
        anomaliesText.setText(anomaliesBuilder.toString());
    }
    
    private void updateCategoryPieChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        Map<String, Double> categoryTotals = new HashMap<>();
        
        for (Transaction transaction : repository.getAllTransactions()) {
            if (!transaction.getCategory().isIncome()) { // Only count expenses
                String category = transaction.getCategory().name();
                categoryTotals.merge(category, transaction.getAmount(), Double::sum);
            }
        }
        
        categoryTotals.forEach((category, total) -> 
            pieChartData.add(new PieChart.Data(category, total))
        );
        
        categoryPieChart.setData(pieChartData);
    }
    
    private void updateMonthlyBarChart() {
        monthlyBarChart.getData().clear();
        
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");
        
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expense");
        
        Map<String, Double> monthlyIncome = new HashMap<>();
        Map<String, Double> monthlyExpense = new HashMap<>();
        
        for (Transaction transaction : repository.getAllTransactions()) {
            String month = transaction.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (transaction.getCategory().isIncome()) {
                monthlyIncome.merge(month, transaction.getAmount(), Double::sum);
            } else {
                monthlyExpense.merge(month, transaction.getAmount(), Double::sum);
            }
        }
        
        monthlyIncome.forEach((month, amount) -> 
            incomeSeries.getData().add(new XYChart.Data<>(month, amount))
        );
        
        monthlyExpense.forEach((month, amount) -> 
            expenseSeries.getData().add(new XYChart.Data<>(month, amount))
        );
        
        monthlyBarChart.getData().addAll(incomeSeries, expenseSeries);
    }
    
    @FXML
    private void handleBackToMain(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) categoryPieChart.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 