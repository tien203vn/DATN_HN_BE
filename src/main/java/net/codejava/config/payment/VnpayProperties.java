package net.codejava.config.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {
    @NotBlank
    private String payUrl;

    @NotBlank
    private String returnUrl;

    @NotBlank
    private String ipnUrl;

    @NotBlank
    private String tmnCode;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String apiUrl;

    @NotBlank
    private String version = "2.1.0";

    @NotBlank
    private String command = "pay";

    @NotBlank
    private String orderType = "wallet_topup";

    @NotBlank
    private String currCode = "VND";

    @NotBlank
    private String defaultLocale = "vn";

    @NotNull
    private Integer expireMinutes = 15;
}
