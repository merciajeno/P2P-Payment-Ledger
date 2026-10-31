package com.mercia.model;

public enum FraudAlertStatus {
    PENDING,    // waiting for admin review
    DISMISSED,  // admin dismissed it
    BLOCKED     // admin blocked the transaction
}
