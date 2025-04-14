package com.group69.finance.model;

public enum Category {
    // Expense Categories
    GROCERIES("Groceries", false), RENT("Rent", false), UTILITIES("Utilities", false),
    TRANSPORT("Transport", false), ENTERTAINMENT("Entertainment", false), DINING_OUT("Dining Out", false),
    SHOPPING("Shopping", false), HEALTHCARE("Healthcare", false), EDUCATION("Education", false),
    INSURANCE("Insurance", false), OTHER_EXPENSE("Other Expense", false),
    // Income Categories
    SALARY("Salary", true), INVESTMENT("Investment", true), GIFT_RECEIVED("Gift Received", true),
    OTHER_INCOME("Other Income", true),
    // Special Categories
    GIFT_GIVEN("Gift Given", false), UNCATEGORIZED("Uncategorized", false);

    private final String displayName;
    private final boolean isIncome;

    Category(String displayName, boolean isIncome) {
        this.displayName = displayName;
        this.isIncome = isIncome;
    }
    public String getDisplayName() { return displayName; }
    public boolean isIncome() { return isIncome; }
    @Override public String toString() { return displayName; }

    public static Category fromDisplayName(String text) {
        for (Category b : Category.values()) {
            if (b.displayName.equalsIgnoreCase(text)) return b;
        }
        return UNCATEGORIZED;
    }
    public static Category fromName(String name) {
        try { return Category.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return UNCATEGORIZED; }
    }
}