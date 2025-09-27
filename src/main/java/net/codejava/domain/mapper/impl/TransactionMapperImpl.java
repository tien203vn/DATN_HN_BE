package net.codejava.domain.mapper.impl;

import java.util.Date;

import org.springframework.stereotype.Component;

import net.codejava.domain.dto.transaction.AddSystemTransactionRequestDTO;
import net.codejava.domain.dto.transaction.TransactionResponseDTO;
import net.codejava.domain.entity.Transaction;
import net.codejava.domain.enums.TransactionStatus;
import net.codejava.domain.mapper.TransactionMapper;

@Component
public class TransactionMapperImpl implements TransactionMapper {
    @Override
    public Transaction addSystemTransactionRequestToEntity(AddSystemTransactionRequestDTO requestDTO) {
        TransactionStatus status = requestDTO.status() != null ? requestDTO.status() : TransactionStatus.SUCCESS;
        return Transaction.builder()
                .amount(requestDTO.amount())
                .transactionType(requestDTO.transactionType())
                .createdAt(new Date())
                .bookingId(requestDTO.bookingId())
                .carName(requestDTO.carName())
                .user(requestDTO.user())
                .status(status)
                .paymentReference(requestDTO.paymentReference())
                .paymentGateway(requestDTO.paymentGateway())
                .gatewayTransactionNo(requestDTO.gatewayTransactionNo())
                .build();
    }

    @Override
    public TransactionResponseDTO toTransactionResponseDTO(Transaction entity) {
        TransactionStatus status = entity.getStatus() != null ? entity.getStatus() : TransactionStatus.SUCCESS;
        return TransactionResponseDTO.builder()
                .amount(entity.getAmount())
                .transactionType(entity.getTransactionType())
                .transactionTypeTitle(entity.getTransactionType().getTitle())
                .createdAt(entity.getCreatedAt())
                .bookingId(entity.getBookingId())
                .carName(entity.getCarName())
                .status(status)
                .paymentReference(entity.getPaymentReference())
                .paymentGateway(entity.getPaymentGateway())
                .gatewayTransactionNo(entity.getGatewayTransactionNo())
                .build();
    }
}
