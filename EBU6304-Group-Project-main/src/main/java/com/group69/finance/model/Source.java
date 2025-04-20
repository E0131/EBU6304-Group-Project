package com.group69.finance.model;

public enum Source {
    WECHAT_PAY("WeChat Pay"), ALIPAY("Alipay"), BANK_TRANSFER("Bank Transfer"),
    CREDIT_CARD("Credit Card"), DEBIT_CARD("Debit Card"), CASH("Cash"),
    OCTOPUS("Octopus Card"), OTHER("Other");

    private final String displayName;
    Source(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }

    public static Source fromDisplayName(String text) {
        for (Source s : Source.values()) { if (s.displayName.equalsIgnoreCase(text)) return s; }
        return OTHER;
    }
    public static Source fromName(String name) {
        try { return Source.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return OTHER; }
    }
}