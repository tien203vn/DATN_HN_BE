package net.codejava.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
public class AppException extends RuntimeException {
    @Getter
    private Object[] args;

    public AppException() {
        super();
    }

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Object... args) {
        super(message);
        this.args = args;
    }
}
