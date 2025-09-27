package net.codejava;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.RequiredArgsConstructor;
import net.codejava.config.payment.VnpayProperties;

@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties(VnpayProperties.class)
public class AppMain {
    private static final Logger log = LoggerFactory.getLogger(AppMain.class);

    public static void main(String[] args) {
        SpringApplication.run(AppMain.class, args);
        log.info("Application started - Swagger UI: http://localhost:8080/swagger-ui/index.html");
    }
}
