package net.codejava.service;

import java.util.Map;

import net.codejava.domain.dto.payment.VnpayCallbackResponseDTO;
import net.codejava.domain.dto.payment.VnpayCreatePaymentRequestDTO;
import net.codejava.domain.dto.payment.VnpayCreatePaymentResponseDTO;
import net.codejava.domain.dto.payment.VnpayReturnResponseDTO;
import net.codejava.domain.entity.User;

public interface VnpayPaymentService {
    VnpayCreatePaymentResponseDTO createWalletTopUp(
            User user, VnpayCreatePaymentRequestDTO requestDTO, String clientIpAddress);

    VnpayCallbackResponseDTO handleIpnCallback(Map<String, String> params);

    VnpayReturnResponseDTO handleReturnUrl(Map<String, String> params);
}
