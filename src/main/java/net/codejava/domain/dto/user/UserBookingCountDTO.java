package net.codejava.domain.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserBookingCountDTO {
    private UserDetailResponseDTO user;
    private Long bookingCount;
}
