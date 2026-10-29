package com.mercia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "Name is required") 
		@Size(max = 100, message = "Name cannot exceed 100 characters") 
		String name,

		@NotBlank(message = "Email is required") 
		@Email(message = "Enter a valid email address") 
		@Size(max = 254, message = "Email is too long") 
		String email,

		@NotBlank(message = "Phone number is required") 
		@Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Enter a valid phone number") 
		String phoneNumber,

		@NotBlank(message = "Password is required") 
		@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") 
		String password,

		@NotBlank(message = "Transaction PIN is required") 
		@Pattern(regexp = "^\\d{4}$", message = "Transaction PIN must contain exactly 4 digits") 
		String transactionPin

) {}
