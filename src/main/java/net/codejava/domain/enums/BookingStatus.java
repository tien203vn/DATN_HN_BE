package net.codejava.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BookingStatus {
    PENDING_DEPOSIT("Pedning deposit"), // Không dùng
    CONFIRMED("Confirmed"), // Khách đã đặt cọc
    CANCELLED("Cancelled"), // Khách đã hủy
    PICK_UP("Pick up"), // Đã lấy xe
    IN_PROGRESS("In progress"), // Không dùng
    PENDING_PAYMENT("Pending payment"), // Không dùng
    COMPLETED("Completed"); // Đã trả xe
    private final String title;
}
