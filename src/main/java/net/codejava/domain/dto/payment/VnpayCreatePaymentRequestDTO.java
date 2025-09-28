package net.codejava.domain.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VnpayCreatePaymentRequestDTO(
        @NotNull Long amount, String bankCode, String language, String returnUrl) {}
