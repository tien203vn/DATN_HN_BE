package net.codejava.utility;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.codejava.exceptions.AppException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VnpayUtil {
    private static final String HMAC_SHA512 = "HmacSHA512";

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                sb.append(String.format("%02x", aByte & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new AppException("Không thể ký dữ liệu thanh toán");
        }
    }

    public static Map<String, String> sortAndFilterParams(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry ->
                        Objects.nonNull(entry.getValue()) && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> newValue, LinkedHashMap::new));
    }

    public static String buildHashData(Map<String, String> params) {
        return sortAndFilterParams(params).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    public static String buildQueryString(Map<String, String> params) {
        return sortAndFilterParams(params).entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    public static String generateSecureHash(Map<String, String> params, String secretKey) {
        String hashData = buildHashData(params);
        return hmacSHA512(secretKey, hashData);
    }

    public static boolean validateSecureHash(Map<String, String> params, String secretKey, String secureHash) {
        Map<String, String> sanitized = sortAndFilterParams(params);
        sanitized.remove("vnp_SecureHashType");
        sanitized.remove("vnp_SecureHash");
        String calculated = generateSecureHash(sanitized, secretKey);
        return calculated.equalsIgnoreCase(secureHash);
    }

    public static String randomNumeric(int length) {
        String digits = "0123456789";
        StringBuilder builder = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            builder.append(digits.charAt(random.nextInt(digits.length())));
        }
        return builder.toString();
    }
}
