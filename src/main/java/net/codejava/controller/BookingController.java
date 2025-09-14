package net.codejava.controller;

import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.codejava.constant.Endpoint;
import net.codejava.domain.dto.booking.*;
import net.codejava.domain.dto.meta.MetaRequestDTO;
import net.codejava.domain.dto.meta.MetaResponseDTO;
import net.codejava.responses.MetaResponse;
import net.codejava.responses.Response;
import net.codejava.service.BookingService;
import net.codejava.utility.AuthUtil;
import net.codejava.utility.JwtTokenUtil;

@Tag(name = "Booking Controller", description = "APIs related to Booking operations")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookingService;
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping(Endpoint.V1.Booking.GET_LIST_BOOKING)
    public ResponseEntity<MetaResponse<MetaResponseDTO, List<BookingResponseDTO>>> getListBookingForUserManager(
            HttpServletRequest servletRequest, @ParameterObject BookingFilterDTO metaRequestDTO) {
        Integer userId =
                Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getListBookingForUserManager(metaRequestDTO, userId));
    }

    @GetMapping(Endpoint.V1.Booking.GET_LIST_FOR_USER)
    public ResponseEntity<MetaResponse<MetaResponseDTO, List<BookingResponseDTO>>> getListBookingForUser(
            HttpServletRequest servletRequest, @ParameterObject MetaRequestDTO metaRequestDTO) {
        Integer userId =
                Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK).body(bookingService.getListBookingForUser(metaRequestDTO, userId));
    }

    @GetMapping(Endpoint.V1.Booking.GET_LIST_BY_CAR)
    public ResponseEntity<MetaResponse<MetaResponseDTO, List<BookingResponseForOwnerDTO>>> getListBookingByCarId(
            @ParameterObject MetaRequestDTO metaRequestDTO, @PathVariable(name = "carId") Integer carId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getListBookingByCarId(
                        metaRequestDTO, carId, AuthUtil.getRequestedUser().getId()));
    }

    @GetMapping(Endpoint.V1.Booking.GET_DETAIL)
    public ResponseEntity<Response<BookingDetailResponseDTO>> getDetailBooking(
            @PathVariable(name = "id") Integer bookingId) {
        return ResponseEntity.status(HttpStatus.OK).body(bookingService.getDetailBooking(bookingId));
    }

    @PostMapping(Endpoint.V1.Booking.ADD_BOOKING)
    public ResponseEntity<Response<BookingDetailResponseDTO>> addBooking(
            HttpServletRequest servletRequest, @RequestBody @Valid AddBookingRequestDTO requestDTO) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.addBooking(AuthUtil.getRequestedUser().getId(), requestDTO));
    }

    @PutMapping(Endpoint.V1.Booking.UPDATE)
    public ResponseEntity<Response<BookingDetailResponseDTO>> updateBooking(
            @PathVariable("id") Integer bookingId, @RequestBody @Valid UpdBookingRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(bookingService.updateBooking(bookingId, requestDTO));
    }

    @PatchMapping(Endpoint.V1.Booking.CONFIRM_DEPOSIT)
    public ResponseEntity<Response<String>> confirmDeposit(@PathVariable(name = "id") Integer bookingId)
            throws MessagingException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.confirmDeposit(
                        bookingId, AuthUtil.getRequestedUser().getId()));
    }

    @PatchMapping(Endpoint.V1.Booking.CONFIRM_PICK_UP)
    public ResponseEntity<Response<String>> confirmPickUp(@PathVariable(name = "id") Integer bookingId) {
        return ResponseEntity.status(HttpStatus.OK).body(bookingService.confirmPickUp(bookingId));
    }

    @PatchMapping(Endpoint.V1.Booking.CONFIRM_PAYMENT)
    public ResponseEntity<Response<String>> confirmPayment(@PathVariable(name = "id") Integer bookingId
            // ,
            // @RequestParam(name = "payment-method") @Valid @NotBlank(message = "Payment Method is not empty") String
            // paymentMethod
            ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.confirmPayment(AuthUtil.getRequestedUser().getId(), bookingId));
        //        try {
        //            PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
        //            return ResponseEntity.status(HttpStatus.OK).body(bookingService.confirmPayment(bookingId));
        //        } catch (IllegalArgumentException e) {
        //            throw new AppException("Invalid Payment method");
        //        }
    }

//    @PatchMapping(Endpoint.V1.Booking.CANCELLED_BOOKING)
//    public ResponseEntity<Response<String>> cancelBooking(@PathVariable(name = "id") Integer bookingId)
//            throws MessagingException {
//        return ResponseEntity.status(HttpStatus.OK)
//                .body(bookingService.cancelBooking(AuthUtil.getRequestedUser().getId(), bookingId));
//    }

    @PatchMapping(Endpoint.V1.Booking.RETURN_CAR)
    public ResponseEntity<Response<String>> returnCar(@PathVariable(name = "id") Integer bookingId)
            throws MessagingException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.returnCar(AuthUtil.getRequestedUser().getId(), bookingId));
    }

    @PostMapping("/return-car-complete")
    public ResponseEntity<String> completeReturnCar(@RequestBody ReturnCarRequestDTO dto) {
        return ResponseEntity.ok(bookingService.completeReturnCar(dto).getMessage());
    }

    @PatchMapping("/api/v1/booking/confirm-booking/{id}")
    public Response<String> confirmBooking(@PathVariable("id") Integer bookingId) {
        return bookingService.confirmBooking(bookingId);
    }

    @PatchMapping("/api/v1/booking/cancel/{id}")
    public ResponseEntity<Response<String>> cancelBooking(@PathVariable("id") Integer bookingId) throws MessagingException {
//        Integer userId = AuthUtil.getRequestedUser().getId();
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.cancelBooking( bookingId));
    }

    @PostMapping("/api/v1/booking/complete/{id}")
    public ResponseEntity<Response<String>> completeBooking(
            @PathVariable("id") Integer bookingId,
            @RequestBody Map<String, Object> payload) {
        String note = (String) payload.get("note");
        Integer lateMinutes = (Integer) payload.get("lateMinutes");
        Double compensationFee = ((Number) payload.get("compensationFee")).doubleValue();
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.completeBooking(bookingId, note, lateMinutes, compensationFee));
    }

    @GetMapping("/api/v1/booking/monthly-summary")
    public ResponseEntity<Response<Map<Integer, Long>>> getMonthlyBookingSummary( HttpServletRequest servletRequest) {
        Integer userId =
                Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyBookingSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-product-summary")
    public ResponseEntity<Response<Map<Integer, Long>>> getMonthlyProductSummary(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyProductSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-customer-summary")
    public ResponseEntity<Response<Map<Integer, Long>>> getMonthlyCustomerSummary(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyCustomerSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-hours-summary")
    public ResponseEntity<Response<Map<Integer, Long>>> getMonthlyHoursSummary(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyHoursSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-status-summary")
    public ResponseEntity<Response<Map<Integer, List<Map<String, Object>>>>> getMonthlyStatusSummary(
            HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyStatusSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-revenue-summary")
    public ResponseEntity<Response<Map<Integer, Double>>> getMonthlyRevenueSummary(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyRevenueSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-repair-cost-summary")
    public ResponseEntity<Response<Map<Integer, Double>>> getMonthlyRepairCostSummary(
            HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyRepairCostSummary(userId));
    }

    @GetMapping("/api/v1/booking/monthly-late-fee-summary")
    public ResponseEntity<Response<Map<Integer, Double>>> getMonthlyLateFeeSummary(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyLateFeeSummary(userId));
    }

    @GetMapping("/api/v1/booking/top-revenue-cars")
    public ResponseEntity<Response<List<Map<String, Object>>>> getTopRevenueCars(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getTopRevenueCars(userId));
    }

    @GetMapping("/api/v1/booking/top-rented-cars")
    public ResponseEntity<Response<List<Map<String, Object>>>> getTopRentedCars(HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getTopRentedCars(userId));
    }

    @GetMapping("/api/v1/booking/monthly-top-revenue-cars")
    public ResponseEntity<Response<Map<Integer, List<Map<String, Object>>>>> getMonthlyTopRevenueCars(
            HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyTopRevenueCars(userId));
    }

    @GetMapping("/api/v1/booking/monthly-top-rented-cars")
    public ResponseEntity<Response<Map<Integer, List<Map<String, Object>>>>> getMonthlyTopRentedCars(
            HttpServletRequest servletRequest) {
        Integer userId = Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(bookingService.getMonthlyTopRentedCars(userId));
    }




}
