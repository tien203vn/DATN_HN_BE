package net.codejava.controller;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.codejava.constant.Endpoint;
import net.codejava.domain.dto.payment.VnpayCallbackResponseDTO;
import net.codejava.domain.dto.payment.VnpayCreatePaymentRequestDTO;
import net.codejava.domain.dto.payment.VnpayCreatePaymentResponseDTO;
import net.codejava.domain.dto.payment.VnpayReturnResponseDTO;
import net.codejava.responses.Response;
import net.codejava.service.VnpayPaymentService;
import net.codejava.utility.AuthUtil;

@Tag(name = "Payment Controller", description = "Tích hợp cổng thanh toán VNPAY")
@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final VnpayPaymentService vnpayPaymentService;

    @PostMapping(value = Endpoint.V1.Payment.VNPAY_CREATE, consumes = MediaType.APPLICATION_JSON_VALUE)
//    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Response<VnpayCreatePaymentResponseDTO>> createWalletTopUp(
            @Valid @RequestBody VnpayCreatePaymentRequestDTO requestDTO, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        VnpayCreatePaymentResponseDTO responseDTO =
                vnpayPaymentService.createWalletTopUp(AuthUtil.getRequestedUser(), requestDTO, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Response.successfulResponse(
                        HttpStatus.CREATED.value(), "Tạo liên kết thanh toán thành công", responseDTO));
    }

    @GetMapping(value = Endpoint.V1.Payment.VNPAY_IPN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VnpayCallbackResponseDTO> handleIpn(HttpServletRequest servletRequest) {
        VnpayCallbackResponseDTO responseDTO = vnpayPaymentService.handleIpnCallback(extractParams(servletRequest));
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping(value = Endpoint.V1.Payment.VNPAY_RETURN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<VnpayReturnResponseDTO>> handleReturn(HttpServletRequest servletRequest) {
        VnpayReturnResponseDTO responseDTO = vnpayPaymentService.handleReturnUrl(extractParams(servletRequest));
        return ResponseEntity.status(HttpStatus.OK)
                .body(Response.successfulResponse("Đã ghi nhận kết quả giao dịch", responseDTO));
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        if (request.getParameterMap().isEmpty()) {
            return Collections.emptyMap();
        }
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-FORWARDED-FOR");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
