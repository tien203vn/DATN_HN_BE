package net.codejava.service;

import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;

import net.codejava.domain.dto.booking.*;
import net.codejava.domain.dto.meta.MetaRequestDTO;
import net.codejava.domain.dto.meta.MetaResponseDTO;
import net.codejava.domain.entity.Booking;
import net.codejava.responses.MetaResponse;
import net.codejava.responses.Response;

public interface BookingService {
    Booking verifyBookingOwner(Integer ownerId, Integer bookingId);

    Booking verifyBookingCustomer(Integer customerId, Integer bookingId);

    MetaResponse<MetaResponseDTO, List<BookingResponseDTO>> getListBookingForUser(
            MetaRequestDTO requestDTO, Integer userId);

    MetaResponse<MetaResponseDTO, List<BookingResponseDTO>> getListBookingForUserManager(
            BookingFilterDTO requestDTO, Integer userId);


    MetaResponse<MetaResponseDTO, List<BookingResponseForOwnerDTO>> getListBookingByCarId(
            MetaRequestDTO requestDTO, Integer carId, Integer userId);

    Response<BookingDetailResponseDTO> getDetailBooking(Integer bookingId);

    Response<BookingDetailResponseDTO> addBooking(Integer userId, AddBookingRequestDTO requestDTO);

    Response<BookingDetailResponseDTO> updateBooking(Integer bookingId, UpdBookingRequestDTO requestDTO);

    Response<String> confirmDeposit(Integer bookingId, Integer userId) throws MessagingException;

    Response<String> confirmPickUp(Integer bookingId);

    Response<String> confirmPayment(Integer userId, Integer bookingId);

    Response<String> cancelBooking(Integer bookingId) throws MessagingException;

    Response<String> returnCar(Integer userId, Integer bookingId) throws MessagingException;

    Response<String> completeReturnCar(ReturnCarRequestDTO dto);

    void syncCancelStatus();

    void syncCarBookingComplete();

    Response<String> confirmBooking(Integer bookingId);

    Response<String> completeBooking(Integer bookingId, String note, Integer lateMinutes, Double compensationFee);

    Response<Map<Integer, Long>> getMonthlyBookingSummary(Integer userId);

    Response<Map<Integer, Long>> getMonthlyProductSummary(Integer userId);

    Response<Map<Integer, Long>> getMonthlyCustomerSummary(Integer userId);

    Response<Map<Integer, Long>> getMonthlyHoursSummary(Integer userId);

    Response<Map<Integer, List<Map<String, Object>>>> getMonthlyStatusSummary(Integer userId);

    Response<Map<Integer, Double>> getMonthlyRevenueSummary(Integer userId);

    Response<Map<Integer, Double>> getMonthlyRepairCostSummary(Integer userId);

    Response<Map<Integer, Double>> getMonthlyLateFeeSummary(Integer userId);

    Response<List<Map<String, Object>>> getTopRevenueCars(Integer userId);

    Response<List<Map<String, Object>>> getTopRentedCars(Integer userId);

    Response<Map<Integer, List<Map<String, Object>>>> getMonthlyTopRevenueCars(Integer userId);

    Response<Map<Integer, List<Map<String, Object>>>> getMonthlyTopRentedCars(Integer userId);
}
