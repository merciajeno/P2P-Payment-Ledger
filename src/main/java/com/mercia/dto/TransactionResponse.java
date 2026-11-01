package com.mercia.dto;

import com.mercia.enums.TransactionStatus;

public record TransactionResponse(String requestId, TransactionStatus transactionStatus) {

}
