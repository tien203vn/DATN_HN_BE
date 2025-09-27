package net.codejava.domain.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record VnpayCallbackResponseDTO(
        @JsonProperty("RspCode") String rspCode, @JsonProperty("Message") String message) {}
