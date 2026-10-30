package com.mercia.dto;

import java.math.BigDecimal;

public record TransactionRequest(String receiverEmail, BigDecimal amount) {
}