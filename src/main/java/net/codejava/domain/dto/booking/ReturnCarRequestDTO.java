package net.codejava.domain.dto.booking;

import lombok.Data;

@Data
public class ReturnCarRequestDTO {
    private Integer bookingId;
    private String note;
    private Integer lateMinute;
    private Double compensationFee;
}
