package com.group69.finance.service;

import com.group69.finance.model.Category;
import com.group69.finance.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CategorizationService {
    private static final Logger log = LoggerFactory.getLogger(CategorizationService.class);

    /**
     * Simulates AI categorization based on simple keyword matching.
     * In a real application, this would involve a more complex model or API call.
     * @param transaction The transaction to categorize (description is key).
     * @return The suggested Category.
     */
    public Category suggestCategory(Transaction transaction) {
        String description = transaction.getDescription().toLowerCase();
        double amount = transaction.getAmount();
        log.debug("Suggesting category for description: '{}', amount: {}", description, amount);

        // --- Income Rules (only apply if amount is positive) ---
        if (amount >= 0) {
            if (description.contains("salary") || description.contains("wages") || description.contains("工资")) return Category.SALARY;
            if (description.contains("red packet") || description.contains("hongbao") || description.contains("红包") || description.contains("gift received")) return Category.GIFT_RECEIVED; // Treat received red packets as income/gift
            if (description.contains("invest") || description.contains("dividend") || description.contains("interest")) return Category.INVESTMENT;
            // Add more income rules here...
        }

        // --- Expense Rules (apply regardless of sign, but should match expense categories) ---
        if (description.contains("grocery") || description.contains("supermarket") || description.contains("market") || description.contains("菜市场")) return Category.GROCERIES;
        if (description.contains("rent") || description.contains("房租")) return Category.RENT;
        if (description.contains("utility") || description.contains("electricity") || description.contains("water") || description.contains("gas") || description.contains("internet") || description.contains("水电煤")) return Category.UTILITIES;
        if (description.contains("transport") || description.contains("metro") || description.contains("subway") || description.contains("bus") || description.contains("taxi") || description.contains("didi") || description.contains("交通") || description.contains("地铁") || description.contains("公交")) return Category.TRANSPORT;
        if (description.contains("movie") || description.contains("cinema") || description.contains("concert") || description.contains("game") || description.contains("ktv") || description.contains("娱乐")) return Category.ENTERTAINMENT;
        if (description.contains("restaurant") || description.contains("cafe") || description.contains("lunch") || description.contains("dinner") || description.contains("coffee") || description.contains("外卖") || description.contains("吃饭")) return Category.DINING_OUT;
        if (description.contains("clothes") || description.contains("shoes") || description.contains("taobao") || description.contains("jd.com") || description.contains("pdd") || description.contains("淘宝") || description.contains("京东") || description.contains("拼多多") || description.contains("购物")) return Category.SHOPPING;
        if (description.contains("doctor") || description.contains("hospital") || description.contains("pharmacy") || description.contains("药") || description.contains("医院")) return Category.HEALTHCARE;
        if (description.contains("gift") || (description.contains("red packet") && amount < 0) || (description.contains("红包") && amount < 0) ) return Category.GIFT_GIVEN; // Sent red packets as expense

        // --- Default ---
        Category defaultCategory = (amount >= 0) ? Category.OTHER_INCOME : Category.OTHER_EXPENSE;
        log.debug("No specific category rule matched. Returning default: {}", defaultCategory);
        return defaultCategory; // Return default based on amount sign if no rules match
    }
}