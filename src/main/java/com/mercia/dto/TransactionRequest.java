package com.mercia.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
		@NotBlank @Email String receiverEmail,
		@NotNull @Positive BigDecimal amount,
		@NotNull @NotBlank String requestId,
		
		@NotBlank
		 @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
        String pin,
        String remark) {
}