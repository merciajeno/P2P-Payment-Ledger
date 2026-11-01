package com.mercia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mercia.enums.WalletStatus;

public record AdminUserResponse(
        Long userId,
        String name,
        String email,
        String phoneNumber,
        BigDecimal walletBalance,
        WalletStatus walletStatus,
        Instant registeredAt
) {}
