package net.codejava.domain.dto.transaction;

import java.util.Date;

import lombok.Builder;
import net.codejava.domain.enums.TransactionStatus;
import net.codejava.domain.enums.TransactionType;

@Builder
public record TransactionResponseDTO(
        Double amount,
        TransactionType transactionType,
        String transactionTypeTitle,
        Date createdAt,
        Integer bookingId,
        String carName,
        TransactionStatus status,
        String paymentReference,
        String paymentGateway,
        String gatewayTransactionNo) {}
