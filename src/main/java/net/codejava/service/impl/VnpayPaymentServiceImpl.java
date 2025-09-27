package net.codejava.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import net.codejava.config.payment.VnpayProperties;
import net.codejava.domain.dto.payment.VnpayCallbackResponseDTO;
import net.codejava.domain.dto.payment.VnpayCreatePaymentRequestDTO;
import net.codejava.domain.dto.payment.VnpayCreatePaymentResponseDTO;
import net.codejava.domain.dto.payment.VnpayReturnResponseDTO;
import net.codejava.domain.dto.transaction.AddSystemTransactionRequestDTO;
import net.codejava.domain.entity.Transaction;
import net.codejava.domain.entity.User;
import net.codejava.domain.enums.TransactionStatus;
import net.codejava.domain.enums.TransactionType;
import net.codejava.domain.mapper.TransactionMapper;
import net.codejava.exceptions.AppException;
import net.codejava.repository.TransactionRepository;
import net.codejava.repository.UserRepository;
import net.codejava.service.VnpayPaymentService;
import net.codejava.utility.VnpayUtil;

@Service
@RequiredArgsConstructor
public class VnpayPaymentServiceImpl implements VnpayPaymentService {
    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentServiceImpl.class);
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Pattern ORDER_INFO_PATTERN = Pattern.compile("WALLET_TOPUP_USER_(\\d+)_AMOUNT_(\\d+)");
    private static final String PAYMENT_GATEWAY = "VNPAY";
    private static final double VND_SCALE = 100d;
    private static final String PARAM_SECURE_HASH = "vnp_SecureHash";
    private static final String PARAM_ORDER_INFO = "vnp_OrderInfo";
    private static final String PARAM_TXN_REF = "vnp_TxnRef";
    private static final String PARAM_RESPONSE_CODE = "vnp_ResponseCode";
    private static final String PARAM_TRANSACTION_STATUS = "vnp_TransactionStatus";
    private static final String PARAM_TRANSACTION_NO = "vnp_TransactionNo";
    private static final String PARAM_AMOUNT = "vnp_Amount";

    private final VnpayProperties properties;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public VnpayCreatePaymentResponseDTO createWalletTopUp(
            User user, VnpayCreatePaymentRequestDTO requestDTO, String clientIpAddress) {
        if (requestDTO.amount() == null || requestDTO.amount() <= 0) {
            throw new AppException("Số tiền nạp ví phải lớn hơn 0 VND");
        }
        long amount = requestDTO.amount();
        String transactionReference = buildTransactionReference(user.getId());
        LocalDateTime now = LocalDateTime.now(VNPAY_ZONE);
        LocalDateTime expireAt = now.plusMinutes(properties.getExpireMinutes());

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_Command", properties.getCommand());
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put(PARAM_AMOUNT, String.valueOf(amount * (long) VND_SCALE));
        params.put("vnp_CurrCode", properties.getCurrCode());
        if (StringUtils.hasText(requestDTO.bankCode())) {
            params.put("vnp_BankCode", requestDTO.bankCode());
        }
        params.put(PARAM_TXN_REF, transactionReference);
        params.put(PARAM_ORDER_INFO, buildOrderInfo(user.getId(), amount));
        params.put("vnp_OrderType", properties.getOrderType());
        String locale =
                StringUtils.hasText(requestDTO.language()) ? requestDTO.language() : properties.getDefaultLocale();
        params.put("vnp_Locale", locale);
        params.put(
                "vnp_ReturnUrl",
                StringUtils.hasText(requestDTO.returnUrl()) ? requestDTO.returnUrl() : properties.getReturnUrl());
        params.put("vnp_IpAddr", StringUtils.hasText(clientIpAddress) ? clientIpAddress : "127.0.0.1");
        params.put("vnp_CreateDate", now.format(VNPAY_TIME_FORMAT));
        params.put("vnp_ExpireDate", expireAt.format(VNPAY_TIME_FORMAT));

        String queryString = VnpayUtil.buildQueryString(params);
        String secureHash = VnpayUtil.generateSecureHash(params, properties.getSecretKey());
        String paymentUrl = properties.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

        return VnpayCreatePaymentResponseDTO.builder()
                .paymentUrl(paymentUrl)
                .transactionReference(transactionReference)
                .amount(amount)
                .expireAt(expireAt.atZone(VNPAY_ZONE).toOffsetDateTime().format(ISO_FORMAT))
                .build();
    }

    @Override
    @Transactional
    public VnpayCallbackResponseDTO handleIpnCallback(Map<String, String> params) {
        PaymentProcessingResult result = processPaymentResult(params);
        return VnpayCallbackResponseDTO.builder()
                .rspCode(result.rspCode())
                .message(result.message())
                .build();
    }

    @Override
    @Transactional
    public VnpayReturnResponseDTO handleReturnUrl(Map<String, String> params) {
        PaymentProcessingResult result = processPaymentResult(params);
        return VnpayReturnResponseDTO.builder()
                .success(result.success())
                .message(result.message())
                .transactionReference(result.transactionReference())
                .amount(result.amount())
                .responseCode(result.responseCode())
                .transactionStatus(result.transactionStatus())
                .build();
    }

    private PaymentProcessingResult processPaymentResult(Map<String, String> params) {
        PaymentProcessingResult signatureError = validateSignature(params);
        if (signatureError != null) {
            return signatureError;
        }

        ContextExtraction context = extractContext(params);
        if (context.error() != null) {
            return context.error();
        }

        Optional<User> optionalUser = userRepository.findById(context.userId());
        if (optionalUser.isEmpty()) {
            log.warn("[VNPAY] User not found for ID {}", context.userId());
            return new PaymentProcessingResult(
                    "01",
                    "Không tìm thấy người dùng",
                    false,
                    context.txnRef(),
                    context.amount(),
                    context.responseCode(),
                    context.transactionStatus());
        }
        User user = optionalUser.get();

        boolean success = isSuccessful(context.responseCode(), context.transactionStatus());
        Optional<Transaction> existingTransactionOpt = transactionRepository.findByPaymentReference(context.txnRef());
        if (existingTransactionOpt.isPresent()) {
            Transaction existingTransaction = existingTransactionOpt.get();
            if (existingTransaction.getStatus() == TransactionStatus.SUCCESS) {
                log.info("[VNPAY] Transaction {} already confirmed", context.txnRef());
                return new PaymentProcessingResult(
                        "02",
                        "Giao dịch đã được xác nhận",
                        true,
                        context.txnRef(),
                        context.amount(),
                        context.responseCode(),
                        context.transactionStatus());
            }
            updateTransaction(existingTransaction, context.amount(), context.transactionNo(), success);
            if (success) {
                increaseWalletBalance(user, context.amount());
            }
            return buildProcessedResult(success, context);
        }

        Transaction newTransaction =
                transactionMapper.addSystemTransactionRequestToEntity(AddSystemTransactionRequestDTO.builder()
                        .amount((double) context.amount())
                        .transactionType(TransactionType.TOP_UP)
                        .bookingId(null)
                        .carName("Nạp ví qua VNPAY")
                        .user(user)
                        .status(success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED)
                        .paymentReference(context.txnRef())
                        .paymentGateway(PAYMENT_GATEWAY)
                        .gatewayTransactionNo(context.transactionNo())
                        .build());
        transactionRepository.save(newTransaction);
        if (success) {
            increaseWalletBalance(user, context.amount());
        }
        return buildProcessedResult(success, context);
    }

    private PaymentProcessingResult validateSignature(Map<String, String> params) {
        String secureHash = params.get(PARAM_SECURE_HASH);
        if (!StringUtils.hasText(secureHash)) {
            return new PaymentProcessingResult("97", "Thiếu chữ ký xác thực", false, null, null, null, null);
        }
        if (!VnpayUtil.validateSecureHash(new HashMap<>(params), properties.getSecretKey(), secureHash)) {
            return new PaymentProcessingResult("97", "Chữ ký không hợp lệ", false, null, null, null, null);
        }
        return null;
    }

    private ContextExtraction extractContext(Map<String, String> params) {
        String txnRef = params.get(PARAM_TXN_REF);
        String responseCode = params.get(PARAM_RESPONSE_CODE);
        String transactionStatus = params.get(PARAM_TRANSACTION_STATUS);
        String transactionNo = params.get(PARAM_TRANSACTION_NO);

        long amount;
        try {
            amount = parseAmountVnd(params.get(PARAM_AMOUNT));
        } catch (NumberFormatException ex) {
            log.warn("[VNPAY] Unable to parse amount: {}", params.get(PARAM_AMOUNT), ex);
            PaymentProcessingResult error = new PaymentProcessingResult(
                    "04", "Số tiền không hợp lệ", false, txnRef, 0L, responseCode, transactionStatus);
            return new ContextExtraction(error, null, 0L, txnRef, transactionNo, responseCode, transactionStatus);
        }

        String orderInfo = params.get(PARAM_ORDER_INFO);
        Matcher matcher = ORDER_INFO_PATTERN.matcher(orderInfo != null ? orderInfo : "");
        if (!matcher.matches()) {
            log.warn("[VNPAY] Order info is invalid: {}", orderInfo);
            PaymentProcessingResult error = new PaymentProcessingResult(
                    "01", "Mã giao dịch không hợp lệ", false, txnRef, amount, responseCode, transactionStatus);
            return new ContextExtraction(error, null, amount, txnRef, transactionNo, responseCode, transactionStatus);
        }

        Integer userId = Integer.valueOf(matcher.group(1));
        long declaredAmount;
        try {
            declaredAmount = Long.parseLong(matcher.group(2));
        } catch (NumberFormatException ex) {
            log.warn("[VNPAY] Declared amount is invalid: {}", matcher.group(2));
            PaymentProcessingResult error = new PaymentProcessingResult(
                    "04", "Số tiền không hợp lệ", false, txnRef, amount, responseCode, transactionStatus);
            return new ContextExtraction(error, userId, amount, txnRef, transactionNo, responseCode, transactionStatus);
        }

        if (declaredAmount != amount) {
            log.warn("[VNPAY] Declared amount {} does not match response amount {}", declaredAmount, amount);
            PaymentProcessingResult error = new PaymentProcessingResult(
                    "04", "Số tiền không khớp", false, txnRef, amount, responseCode, transactionStatus);
            return new ContextExtraction(error, userId, amount, txnRef, transactionNo, responseCode, transactionStatus);
        }

        return new ContextExtraction(null, userId, amount, txnRef, transactionNo, responseCode, transactionStatus);
    }

    private boolean isSuccessful(String responseCode, String transactionStatus) {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }

    private PaymentProcessingResult buildProcessedResult(boolean success, ContextExtraction context) {
        return new PaymentProcessingResult(
                "00",
                success ? "Xác nhận giao dịch thành công" : "Giao dịch thất bại",
                success,
                context.txnRef(),
                context.amount(),
                context.responseCode(),
                context.transactionStatus());
    }

    private void updateTransaction(Transaction transaction, long amount, String transactionNo, boolean success) {
        transaction.setAmount((double) amount);
        transaction.setTransactionType(TransactionType.TOP_UP);
        transaction.setGatewayTransactionNo(transactionNo);
        transaction.setPaymentGateway(PAYMENT_GATEWAY);
        transaction.setStatus(success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
        transaction.setCreatedAt(new Date());
        transactionRepository.save(transaction);
    }

    private void increaseWalletBalance(User user, long amount) {
        double currentWallet = user.getWallet() == null ? 0d : user.getWallet();
        user.setWallet(currentWallet + amount);
        userRepository.save(user);
    }

    private long parseAmountVnd(String amountValue) {
        if (!StringUtils.hasText(amountValue)) {
            return 0;
        }
        return Long.parseLong(amountValue) / (long) VND_SCALE;
    }

    private String buildOrderInfo(Integer userId, long amount) {
        return String.format("WALLET_TOPUP_USER_%d_AMOUNT_%d", userId, amount);
    }

    private String buildTransactionReference(Integer userId) {
        LocalDateTime now = LocalDateTime.now(VNPAY_ZONE);
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        return userId + timestamp + VnpayUtil.randomNumeric(3);
    }

    private record ContextExtraction(
            PaymentProcessingResult error,
            Integer userId,
            long amount,
            String txnRef,
            String transactionNo,
            String responseCode,
            String transactionStatus) {}

    private record PaymentProcessingResult(
            String rspCode,
            String message,
            boolean success,
            String transactionReference,
            Long amount,
            String responseCode,
            String transactionStatus) {}
}
