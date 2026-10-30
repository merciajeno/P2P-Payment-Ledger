package com.mercia.dto;

import com.mercia.model.TransactionStatus;

public record TransactionResponse(String requestId, TransactionStatus transactionStatus) {

}
