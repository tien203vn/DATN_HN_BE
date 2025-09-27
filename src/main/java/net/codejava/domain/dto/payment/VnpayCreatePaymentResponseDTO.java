package net.codejava.domain.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VnpayCreatePaymentResponseDTO(
        String paymentUrl, String transactionReference, Long amount, String expireAt) {}
