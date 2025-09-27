package net.codejava.domain.dto.transaction;

import lombok.Builder;
import net.codejava.domain.entity.User;
import net.codejava.domain.enums.TransactionStatus;
import net.codejava.domain.enums.TransactionType;

@Builder
public record AddSystemTransactionRequestDTO(
        Double amount,
        TransactionType transactionType,
        Integer bookingId,
        String carName,
        User user,
        TransactionStatus status,
        String paymentReference,
        String paymentGateway,
        String gatewayTransactionNo) {}
