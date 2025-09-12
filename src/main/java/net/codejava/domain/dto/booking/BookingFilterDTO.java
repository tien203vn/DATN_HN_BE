package net.codejava.domain.dto.booking;

import lombok.Data;

@Data
public class BookingFilterDTO {
    private int page;
    private int currentPage;
    private int limit = 10;
    private String bookingStatus;
    private String carName;
    private String startDateTime;
    private String endDateTime;
}
