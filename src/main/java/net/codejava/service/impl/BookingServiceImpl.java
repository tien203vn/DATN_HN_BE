package net.codejava.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.mail.MessagingException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.codejava.constant.MailTemplate;
import net.codejava.constant.MetaConstant;
import net.codejava.domain.dto.booking.*;
import net.codejava.domain.dto.booking.ReturnCarRequestDTO;
import net.codejava.domain.dto.meta.MetaRequestDTO;
import net.codejava.domain.dto.meta.MetaResponseDTO;
import net.codejava.domain.dto.meta.SortingDTO;
import net.codejava.domain.dto.transaction.AddSystemTransactionRequestDTO;
import net.codejava.domain.entity.Booking;
import net.codejava.domain.entity.Car;
import net.codejava.domain.entity.User;
import net.codejava.domain.entity.UserInfor;
import net.codejava.domain.enums.BookingStatus;
import net.codejava.domain.enums.PaymentMethod;
import net.codejava.domain.enums.TransactionType;
import net.codejava.domain.enums.UserInforType;
import net.codejava.domain.mapper.BookingMapper;
import net.codejava.exceptions.AppException;
import net.codejava.repository.BookingRepository;
import net.codejava.repository.CarRepository;
import net.codejava.repository.UserRepository;
import net.codejava.responses.MetaResponse;
import net.codejava.responses.Response;
import net.codejava.service.BookingService;
import net.codejava.service.CarService;
import net.codejava.service.TransactionService;
import net.codejava.utility.MailSenderUtil;
import net.codejava.utility.TimeUtil;

@Service
@RequiredArgsConstructor
@EnableTransactionManagement
public class BookingServiceImpl implements BookingService {
    private final CarRepository carRepo;
    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final TransactionService transactionService;
    private final CarService carService;
    private final BookingMapper bookingMapper;
    private final MailSenderUtil mailSenderUtil;

    @Override
    public Booking verifyBookingOwner(Integer ownerId, Integer bookingId) {
        Optional<Booking> findBooking = bookingRepo.findById(bookingId);
        if (findBooking.isEmpty()) throw new AppException("This booking is not existed");
        Booking booking = findBooking.get();
        if (booking.getCar().getCarOwner().getId() != ownerId) throw new AppException("Unauthorized");
        return booking;
    }

    @Override
    public Booking verifyBookingCustomer(Integer customerId, Integer bookingId) {
        Optional<Booking> findBooking = bookingRepo.findById(bookingId);
        if (findBooking.isEmpty()) throw new AppException("This booking is not existed");
        Booking booking = findBooking.get();
        if (booking.getUser().getId() != customerId) throw new AppException("Unauthorized");
        return booking;
    }

    @Override
    public MetaResponse<MetaResponseDTO, List<BookingResponseDTO>> getListBookingForUser(
            MetaRequestDTO metaRequestDTO, Integer userId) {
        Optional<User> findUser = userRepo.findById(userId);
        if (findUser.isEmpty()) throw new AppException("This user is not existed");
        Sort sort = metaRequestDTO.sortDir().equals(MetaConstant.Sorting.DEFAULT_DIRECTION)
                ? Sort.by(metaRequestDTO.sortField()).ascending()
                : Sort.by(metaRequestDTO.sortField()).descending();
        Pageable pageable = PageRequest.of(metaRequestDTO.currentPage(), metaRequestDTO.pageSize(), sort);
        Page<Booking> page = metaRequestDTO.keyword() == null
                ? bookingRepo.getListBookingByUserId(userId, pageable)
                : bookingRepo.getListBookingByUserIdWithKeyword(userId, metaRequestDTO.keyword(), pageable);
        if (page.getContent().isEmpty()) throw new AppException("List booking is empty");
        List<BookingResponseDTO> li = page.getContent().stream()
                .map(temp -> bookingMapper.toBookingResponseDto(temp))
                .toList();
        return MetaResponse.successfulResponse(
                "Get list booking success",
                MetaResponseDTO.builder()
                        .totalItems((int) page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .currentPage(metaRequestDTO.currentPage())
                        .pageSize(metaRequestDTO.pageSize())
                        .sorting(SortingDTO.builder()
                                .sortField(metaRequestDTO.sortField())
                                .sortDir(metaRequestDTO.sortDir())
                                .build())
                        .build(),
                li);
    }

    @Override
    public MetaResponse<MetaResponseDTO, List<BookingResponseDTO>> getListBookingForUserManager(
            BookingFilterDTO requestDTO, Integer userId) {
        Optional<User> findUser = userRepo.findById(userId);
        if (findUser.isEmpty()) throw new AppException("This user is not existed");
        Pageable pageable = PageRequest.of(requestDTO.getCurrentPage(), requestDTO.getLimit());
        BookingStatus status = null;
        if (requestDTO.getBookingStatus() != null) {
            try {
                status = BookingStatus.valueOf(requestDTO.getBookingStatus());
            } catch (IllegalArgumentException e) {
                throw new AppException("Trạng thái booking không hợp lệ");
            }
        }

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (requestDTO.getStartDateTime() != null && !requestDTO.getStartDateTime().isEmpty()) {
            startDateTime = LocalDateTime.parse(requestDTO.getStartDateTime(), formatter);
        }
        if (requestDTO.getEndDateTime() != null && !requestDTO.getEndDateTime().isEmpty()) {
            endDateTime = LocalDateTime.parse(requestDTO.getEndDateTime(), formatter);
        }
        Page<Booking> page = bookingRepo.findBookingsByFilter(
                userId,
                status,
                requestDTO.getCarName(),
                startDateTime,
                endDateTime,
                pageable
        );

        if (page.getContent().isEmpty()) throw new AppException("List booking is empty");

        List<BookingResponseDTO> li = page.getContent().stream()
                .map(bookingMapper::toBookingResponseDto)
                .toList();

        return MetaResponse.successfulResponse(
                "Get list booking for owner success",
                MetaResponseDTO.builder()
                        .totalItems((int) page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .currentPage(requestDTO.getCurrentPage())
                        .pageSize(requestDTO.getLimit())
                        .build(),
                li);
    }

    @Override
    public MetaResponse<MetaResponseDTO, List<BookingResponseForOwnerDTO>> getListBookingByCarId(
            MetaRequestDTO metaRequestDTO, Integer carId, Integer userId) {
        // Car
        Car car = carService.verifyCarOwner(userId, carId);
        Sort sort = metaRequestDTO.sortDir().equals(MetaConstant.Sorting.DEFAULT_DIRECTION)
                ? Sort.by(metaRequestDTO.sortField()).ascending()
                : Sort.by(metaRequestDTO.sortField()).descending();
        Pageable pageable = PageRequest.of(metaRequestDTO.currentPage(), metaRequestDTO.pageSize(), sort);
        Page<Booking> page = metaRequestDTO.keyword() == null
                ? bookingRepo.getListBookingByCarId(carId, pageable)
                : bookingRepo.getListBookingByUserId(carId, pageable);
        // if (page.getContent().isEmpty()) throw new AppException("List booking is empty");
        List<BookingResponseForOwnerDTO> li = page.getContent().stream()
                .map(temp -> bookingMapper.toBookingResponseForOwnerDto(temp))
                .toList();
        return MetaResponse.successfulResponse(
                "Get list booking success",
                MetaResponseDTO.builder()
                        .totalItems((int) page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .currentPage(metaRequestDTO.currentPage())
                        .pageSize(metaRequestDTO.pageSize())
                        .sorting(SortingDTO.builder()
                                .sortField(metaRequestDTO.sortField())
                                .sortDir(metaRequestDTO.sortDir())
                                .build())
                        .build(),
                li);
    }

    @Override
    public Response<BookingDetailResponseDTO> getDetailBooking(Integer bookingId) {
        Optional<Booking> findBooking = bookingRepo.findById(bookingId);
        if (findBooking.isEmpty()) throw new AppException("This booking is not existed");

        return Response.successfulResponse(
                "Get detail booking successful", bookingMapper.toBookingDetailResponseDto(findBooking.get()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<BookingDetailResponseDTO> addBooking(Integer customerId, AddBookingRequestDTO requestDTO) {
        // Định dạng DateTimeFormatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Chuyển đổi startDateTime và endDateTime từ String sang LocalDateTime
        LocalDateTime startDateTime = LocalDateTime.parse(requestDTO.startDateTime(), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(requestDTO.endDateTime(), formatter);

        // Car
        Optional<Car> findCar = carRepo.findById(requestDTO.carId());
        if (findCar.isEmpty()) throw new AppException("This car is not existed");
        Car car = findCar.get();
        // Check the condition of car
        if (!car.getIsActive()) throw new AppException("This car is not active");
        if (car.getIsStopped()) throw new AppException("This car is stopped. Contact with the owner of this car");
        // Check schedule to rent Car
        Optional<Car> checkScheduleCar =
                carRepo.checkScheduleCar(requestDTO.carId(), requestDTO.startDateTime(), requestDTO.endDateTime());
        if (checkScheduleCar.isEmpty()) throw new AppException("Invalid booking schedule. Please try again.");
        // Customer
        Optional<User> findCustomer = userRepo.findById(customerId);
        if (findCustomer.isEmpty()) throw new AppException("This user is not existed");
        User customer = findCustomer.get();
        // Case: Payment Method is My Wallet
        if (requestDTO.paymentMethod() == PaymentMethod.MY_WALLET) {
            if (customer.getWallet() < car.getDeposit()) throw new AppException("Let check your wallet");
            customer.setWallet(customer.getWallet() - car.getDeposit());
            userRepo.save(customer);
        }
        // Owner
        User owner = car.getCarOwner();
        owner.setWallet(owner.getWallet() + car.getDeposit());
        userRepo.save(owner);
        // Booking
        Booking newBooking = bookingMapper.addBookingRequestToBookingEntity(requestDTO);
        newBooking.setCar(car);
        newBooking.setUser(customer);
        newBooking.setStartDateTime(startDateTime);
        newBooking.setEndDateTime(endDateTime);

        // **Tính tiền thuê xe và gán vào cột rental_amount**
        long hours = TimeUtil.getHoursDifference(startDateTime, endDateTime);
        double rentalAmount = hours * car.getBasePrice() /24;
        newBooking.setRental_amount(rentalAmount);

        // Add Renter and Driver
        UserInfor renter = requestDTO.userInfors()[0];
        newBooking.addUserInfor(renter);
        UserInfor driver;
        if (requestDTO.userInfors().length == 1) {
            driver = UserInfor.builder()
                    .username(renter.getUsername())
                    .email(renter.getEmail())
                    .phoneNumber(renter.getPhoneNumber())
                    .address(renter.getAddress())
                    .nationalId(renter.getNationalId())
                    .drivingLicense(renter.getDrivingLicense())
                    .userInforType(UserInforType.DRIVER)
                    .birthDay(renter.getBirthDay())
                    .booking(renter.getBooking())
                    .build();
        } else driver = requestDTO.userInfors()[1];
        newBooking.addUserInfor(driver);
        try {
            Booking saveBooking = bookingRepo.save(newBooking);
            // Transaction
            // Customer
            AddSystemTransactionRequestDTO customerTran = AddSystemTransactionRequestDTO.builder()
                    .amount(-car.getDeposit())
                    .transactionType(TransactionType.PAY_DEPOSIT)
                    .bookingId(saveBooking.getId())
                    .carName(car.getName())
                    .user(customer)
                    .build();
            transactionService.addTransaction(customerTran);
            // Owner
            AddSystemTransactionRequestDTO ownerTran = AddSystemTransactionRequestDTO.builder()
                    .amount(car.getDeposit())
                    .transactionType(TransactionType.RECEIVE_DEPOSIT)
                    .bookingId(saveBooking.getId())
                    .carName(car.getName())
                    .user(owner)
                    .build();
            transactionService.addTransaction(ownerTran);
            // Update isAvailable of Car
            car.setIsAvailable(false);
            carRepo.save(car);
            // Send Mail To Owner
            String toMail = owner.getEmail();
            String subject = MailTemplate.RENT_A_CAR.RENT_A_CAR_SUBJECT;
            String template = MailTemplate.RENT_A_CAR.RENT_A_CAR_TEMPLATE;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter mailFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String bookingTime = now.format(mailFormatter).toString();
            Map<String, Object> variable = Map.of(
                    "carName", car.getName(),
                    "bookingTime", bookingTime,
                    "bookingId", saveBooking.getId());
            mailSenderUtil.sendMailWithHTML(toMail, subject, template, variable);
            return Response.successfulResponse(
                    "Add new booking successful", bookingMapper.toBookingDetailResponseDto(saveBooking));
        } catch (Exception e) {
            throw new AppException("Add a new booking fail");
        }
    }
    //    @Override
    //    @Transactional
    //    public Response<String> confirmPickUpCar( Integer bookingId) {
    //        if (booking.getStatus() == BookingStatus.CONFIRMED) {
    //            booking.setStatus(BookingStatus.PICK_UP);
    //            bookingRepo.save(booking);
    //            return Response.successfulResponse("Khách hàng đã nhận xe thành công.");
    //        } else {
    //            throw new AppException("Trạng thái đơn hàng không cho phép nhận xe.");
    //        }
    //    }

    @Override
    public Response<BookingDetailResponseDTO> updateBooking(Integer bookingId, UpdBookingRequestDTO requestDTO) {
        Optional<Booking> oldBooking = bookingRepo.findById(bookingId);
        if (oldBooking.isEmpty()) throw new AppException("This booking is not existed");

        // Check status allow to edit
        if (oldBooking.get().getStatus() == BookingStatus.CANCELLED
                || oldBooking.get().getStatus() == BookingStatus.COMPLETED)
            throw new AppException("Don't allow to edit this booking");

        Booking newBooking = bookingMapper.updBookingRequestToBookingEntity(oldBooking.get(), requestDTO);

        try {
            Booking saveBooking = bookingRepo.save(newBooking);
            return Response.successfulResponse(
                    "Update a booking successful", bookingMapper.toBookingDetailResponseDto(saveBooking));
        } catch (Exception e) {
            throw new AppException("Update a booking fail");
        }
    }

    @Override
    @Transactional
    public Response<String> confirmDeposit(Integer bookingId, Integer userId) throws MessagingException {
        Booking booking = this.verifyBookingOwner(userId, bookingId);
        if (booking.getStatus() == BookingStatus.PENDING_DEPOSIT) {
            // Update Booking
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepo.save(booking);
            // Customer
            User customer = booking.getUser();
            // Car
            Car car = booking.getCar();
            // Send Mail To Customer
            String toMail = customer.getEmail();
            String subject = MailTemplate.CONFIRM_DEPOSIT.CONFIRM_DEPOSIT_SUBJECT;
            String template = MailTemplate.CONFIRM_DEPOSIT.CONFIRM_DEPOSIT_TEMPLATE;
            Map<String, Object> variable = Map.of(
                    "carName", car.getName(),
                    "startTime", TimeUtil.formatToString(booking.getStartDateTime()),
                    "endTime", TimeUtil.formatToString(booking.getEndDateTime()));
            mailSenderUtil.sendMailWithHTML(toMail, subject, template, variable);
            // update trạng thái car -> not available
            car.setIsAvailable(false);
            carRepo.save(car);
            return Response.successfulResponse("Confirm deposit successfully");
        } else throw new AppException("The status of this booking does not allow to confirm deposit");
    }

    @Override
    public Response<String> confirmPickUp(Integer bookingId) {
        Optional<Booking> findBooking = bookingRepo.findById(bookingId);
        if (findBooking.isEmpty()) throw new AppException("This booking is not existed");
        Booking booking = findBooking.get();
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepo.save(booking);
            return Response.successfulResponse("Confirm pick up successfully");
        } else throw new AppException("The status of this booking does not allow to confirm pick up");
    }

    @Override
    @Transactional
    public Response<String> confirmPayment(Integer userId, Integer bookingId) {
        // Booking
        Booking booking = this.verifyBookingOwner(userId, bookingId);

        // Check Car
        //        Optional<Car> findCar = carRepo.findById(booking.getCar().getId());
        //        if (findCar.isEmpty()) throw new AppException("This car is not existed");
        //        if (findCar.get().getIsAvailable() == false) throw new AppException("This car is unavailable");
        //        Car car = findCar.get();

        if (booking.getStatus() == BookingStatus.CONFIRMED) throw new AppException("This booking is completed");
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            //            booking.setPaymentMethod(paymentMethod);
            //            // Case: Payment Method is My Wallet
            //            if (paymentMethod == PaymentMethod.MY_WALLET) {
            //                Double debt = booking.getTotal() - car.getDeposit();
            //                if (debt <= 0) throw new AppException("This booking has been paid");
            //
            //                if (user.getWallet() < debt) throw new AppException("Let check your wallet");
            //                user.setWallet(user.getWallet() - car.getDeposit());
            //                userRepo.save(user);
            //            }
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepo.save(booking);
            return Response.successfulResponse("Confirm payment successfully");
        } else throw new AppException("The status of this booking does not allow to confirm payment");
    }

    @Override
    @Transactional
    public Response<String> cancelBooking(Integer bookingId) throws MessagingException {
        // Lấy thông tin booking
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new AppException("This booking is not existed"));

        // Kiểm tra trạng thái booking
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_DEPOSIT) {
            throw new AppException("Trạng thái đơn hàng không cho phép hủy.");
        }

        // Hoàn tiền vào ví của khách hàng
        User customer = booking.getUser();
        Car car = booking.getCar();
        double deposit = car.getDeposit();
        customer.setWallet(customer.getWallet() + deposit);
        userRepo.save(customer);

        // Cập nhật trạng thái booking
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        // Cập nhật trạng thái xe
        car.setIsAvailable(true);
        carRepo.save(car);

        // Gửi email thông báo hủy đơn hàng
        String toMail = car.getCarOwner().getEmail();
        String subject = "Thông báo hủy đơn hàng";
        String template = MailTemplate.CANCEL_BOOKING.CANCEL_BOOKING_TEMPLATE;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String cancelTime = now.format(formatter);
        Map<String, Object> variable = Map.of("carName", car.getName(), "cancelTime", cancelTime);
        mailSenderUtil.sendMailWithHTML(toMail, subject, template, variable);

        return Response.successfulResponse("Đơn hàng đã được hủy thành công và tiền đặt cọc đã được hoàn vào ví.");
    }

    @Override
    @Transactional
    public Response<String> returnCar(Integer userId, Integer bookingId) throws MessagingException {
        // Booking
        Booking booking = this.verifyBookingCustomer(userId, bookingId);
        // Car
        Car car = booking.getCar();
        // Owner
        User owner = booking.getCar().getCarOwner();
        // Customer
        User customer = booking.getUser();
        // Check status
        if (booking.getStatus() != BookingStatus.IN_PROGRESS)
            throw new AppException("The status of this booking does not allow to return a car");
        // Check Total and Deposit
        Double money = car.getDeposit() - booking.getTotal();
        String message;
        if (money < 0) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            try {
                bookingRepo.save(booking);
            } catch (Exception e) {
                throw new AppException("Return car fail");
            }
            message = "Rent is greater than deposit is " + (-money) + " . Next confirm payment step";
        } else {
            try {
                // Return deposit and pay rent
                // Owner and Transaction
                Double refundMoney = car.getDeposit() - booking.getTotal();
                if (owner.getWallet() < refundMoney)
                    throw new AppException("Return a car fail. The car owner's wallet has no money");
                owner.setWallet(owner.getWallet() - refundMoney);
                User saveOwner = userRepo.save(owner);
                AddSystemTransactionRequestDTO ownerTran = AddSystemTransactionRequestDTO.builder()
                        .amount(-refundMoney)
                        .transactionType(TransactionType.OFFSET_FINAL_PAYMENT)
                        .bookingId(booking.getId())
                        .carName(car.getName())
                        .user(saveOwner)
                        .build();
                transactionService.addTransaction(ownerTran);
                // Customer and Transaction
                customer.setWallet(customer.getWallet() + refundMoney);
                User saveCustomer = userRepo.save(customer);
                AddSystemTransactionRequestDTO customerTran = AddSystemTransactionRequestDTO.builder()
                        .amount(refundMoney)
                        .transactionType(TransactionType.OFFSET_FINAL_PAYMENT)
                        .bookingId(booking.getId())
                        .carName(car.getName())
                        .user(saveCustomer)
                        .build();
                transactionService.addTransaction(customerTran);
                // Set Status for Booking
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepo.save(booking);
            } catch (Exception e) {
                throw new AppException("Return car fail");
            }
            message = "Please confirm to return the car. The exceeding amount of XXXXX " + money
                    + " VND will be returned to your wallet.";
        }
        // Send Mail For Owner
        String toMail = owner.getEmail();
        String subject = MailTemplate.RETURN_A_CAR.RETURN_A_CAR_SUBJECT;
        String template = MailTemplate.RETURN_A_CAR.RETURN_A_CAR_TEMPLATE;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String returnTime = now.format(formatter).toString();
        Map<String, Object> variable = Map.of("carName", car.getName(), "returnTime", returnTime);
        mailSenderUtil.sendMailWithHTML(toMail, subject, template, variable);

        return Response.successfulResponse(message);
    }

    // Hàm được gọi khi chủ xe xác nhận đã kiểm tra xe và hoàn tất việc trả xe
    @Transactional
    public Response<String> completeReturnCar(ReturnCarRequestDTO dto) {
        Optional<Booking> findBooking = bookingRepo.findById(dto.getBookingId());
        if (findBooking.isEmpty()) throw new AppException("Booking không tồn tại");
        Booking booking = findBooking.get();

        if (booking.getStatus() != BookingStatus.PICK_UP)
            throw new AppException("Trạng thái booking không cho phép trả xe");

        booking.setLateMinute(dto.getLateMinute() != null ? dto.getLateMinute() : 0);

        long numberOfHour = booking.getNumberOfHour();
        double extraFee = 0;
        if (dto.getLateMinute() != null && dto.getLateMinute() > 0 && numberOfHour > 0) {
            double hourlyRate = booking.getTotal() / numberOfHour;
            extraFee = Math.ceil(dto.getLateMinute() / 60.0) * hourlyRate * 2;
        }
        booking.setExtraFee(extraFee);

        if (dto.getNote() != null && !dto.getNote().isEmpty()) {
            booking.setNote(dto.getNote());
            booking.setCompensationFee(dto.getCompensationFee() != null ? dto.getCompensationFee() : 0);
        } else {
            booking.setNote(null);
            booking.setCompensationFee(0.0);
        }

        booking.setStatus(BookingStatus.COMPLETED);

        Car car = booking.getCar();
        car.setIsStopped(true);
        carRepo.save(car);
        bookingRepo.save(booking);

        return Response.successfulResponse("Trả xe và kiểm tra xe thành công.");
    }

    // logic job

    /**
     * Quét nếu quá thời gian đặt mà chưa confirm thì chuyển trạng thái hủy và -> available
     */
    @Transactional
    public void syncCancelStatus() {
        LocalDateTime now = LocalDateTime.now();
        //        // Quét các booking PENDING_DEPOSIT quá hạn
        //        List<Booking> pendingBookings =
        // bookingRepo.findAllByStatusAndStartDateTimeBefore(BookingStatus.CONFIRMED, now);
        //        for (Booking booking : pendingBookings) {
        //            booking.setStatus(BookingStatus.CANCELLED);
        //            Car car = booking.getCar();
        //            car.setIsAvailable(true);
        //            carRepo.save(car);
        //            bookingRepo.save(booking);
        //        }

        // Quét các booking CONFIRMED quá hạn 30 phút chưa nhận xe
        List<Booking> confirmedBookings = bookingRepo.findAllByStatus(BookingStatus.CONFIRMED);
        for (Booking booking : confirmedBookings) {
            LocalDateTime pickUpDeadline = booking.getStartDateTime().plusMinutes(30);
            if (now.isAfter(pickUpDeadline)) {
                booking.setStatus(BookingStatus.CANCELLED);
                Car car = booking.getCar();
                car.setIsAvailable(true);
                carRepo.save(car);
                bookingRepo.save(booking);

                // Gửi mail cho chủ xe
                String toMail = car.getCarOwner().getEmail();
                String subject = "Thông báo huỷ đơn đặt xe";
                String template = MailTemplate.CANCEL_BOOKING.CANCEL_BOOKING_TEMPLATE;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String cancelTime = now.format(formatter);
                Map<String, Object> variable = Map.of("carName", car.getName(), "cancelTime", cancelTime);
                try {
                    mailSenderUtil.sendMailWithHTML(toMail, subject, template, variable);
                } catch (jakarta.mail.MessagingException e) {
                    System.err.println("Gửi mail thất bại: " + e.getMessage());
                }
            }
        }
        System.out.println("[SYNC] Đã đồng bộ trạng thái booking bị huỷ do quá hạn nhận xe tại " + now);
    }

    /**
     * * Neu thời gian trả quá hạn thì chuyển trạng thái hoàn thành và -> not available, isactive -> not active
     */
    public void syncCarBookingComplete() {
        System.out.println("[SYNC] Đồng bộ car-booking tại ");
    }

    @Override
    @Transactional
    public Response<String> confirmBooking(Integer bookingId) {
        Optional<Booking> findBooking = bookingRepo.findById(bookingId);
        if (findBooking.isEmpty()) {
            throw new AppException("Booking không tồn tại");
        }

        Booking booking = findBooking.get();
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException("Trạng thái booking không cho phép chuyển sang PICK_UP");
        }

        booking.setStatus(BookingStatus.PICK_UP);
        bookingRepo.save(booking);

        return Response.successfulResponse("Đơn hàng đã chuyển sang trạng thái PICK_UP thành công.");
    }

    @Override
    @Transactional
    public Response<String> completeBooking(Integer bookingId, String note, Integer lateMinutes, Double compensationFee) {
        Optional<Booking> findBooking = bookingRepo.findById(bookingId);
        if (findBooking.isEmpty()) {
            throw new AppException("Booking không tồn tại");
        }

        Booking booking = findBooking.get();
        if (booking.getStatus() != BookingStatus.PICK_UP) {
            throw new AppException("Trạng thái booking không cho phép chuyển sang COMPLETED");
        }

        // Tính tiền phạt dựa trên số phút muộn
        double extraFee = 0.0;
        if (lateMinutes != null && lateMinutes > 0) {
            long numberOfHours = booking.getNumberOfHour();
            if (numberOfHours > 0) {
                double hourlyRate = booking.getTotal() / numberOfHours;
                extraFee = Math.ceil(lateMinutes / 60.0) * hourlyRate * 2;
            }
        }

        // Cập nh���t thông tin booking
        booking.setNote(note);
        booking.setLateMinute(lateMinutes != null ? lateMinutes : 0);
        booking.setCompensationFee(compensationFee != null ? compensationFee : 0.0);
        booking.setExtraFee(extraFee);
        booking.setStatus(BookingStatus.COMPLETED);

        // Cập nhật trạng thái xe
        Car car = booking.getCar();
        car.setIsAvailable(false);
        car.setIsStopped(true);
        carRepo.save(car);

        bookingRepo.save(booking);

        return Response.successfulResponse("Đơn hàng đã chuyển sang trạng thái COMPLETED thành công.");
    }

    @Override
    public Response<Map<Integer, Long>> getMonthlyBookingSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.countBookingsByMonth(currentYear, userId);

        // Khởi tạo Map với 12 tháng, giá trị mặc định là 0
        Map<Integer, Long> monthlySummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummary.put(month, 0L);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Long count = (Long) result[1];
            monthlySummary.put(month, count);
        }

        return Response.successfulResponse("Lấy tổng số đơn hàng theo từng tháng thành công.", monthlySummary);
    }

    @Override
    public Response<Map<Integer, Long>> getMonthlyProductSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.countProductsByMonthForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, giá trị mặc định là 0
        Map<Integer, Long> monthlySummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummary.put(month, 0L);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Long count = (Long) result[1];
            monthlySummary.put(month, count);
        }

        return Response.successfulResponse("Lấy tổng số sản phẩm cho thuê theo từng tháng thành công.", monthlySummary);
    }

    @Override
    public Response<Map<Integer, Long>> getMonthlyCustomerSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.countCustomersByMonthForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, giá trị mặc định là 0
        Map<Integer, Long> monthlySummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummary.put(month, 0L);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Long count = (Long) result[1];
            monthlySummary.put(month, count);
        }

        return Response.successfulResponse("Lấy tổng số người dùng đã thuê xe theo từng tháng thành công.", monthlySummary);
    }

    @Override
    public Response<Map<Integer, Long>> getMonthlyHoursSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.countHoursByMonthForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, gi�� trị mặc định là 0
        Map<Integer, Long> monthlySummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummary.put(month, 0L);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Long totalHours = (Long) result[1];
            monthlySummary.put(month, totalHours);
        }

        return Response.successfulResponse("Lấy tổng số giờ thuê xe theo từng tháng thành công.", monthlySummary);
    }

    @Override
    public Response<Map<Integer, List<Map<String, Object>>>> getMonthlyStatusSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.countBookingsByStatusAndMonthForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, mỗi tháng là một danh sách trạng thái
        Map<Integer, List<Map<String, Object>>> monthlyStatusSummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyStatusSummary.put(month, new ArrayList<>());
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            String status = result[1].toString();
            Long count = (Long) result[2];

            monthlyStatusSummary.get(month).add(Map.of(
                    "name", status,
                    "value", count
            ));
        }

        return Response.successfulResponse("Lấy tổng số đơn hàng theo trạng thái và tháng thành công.", monthlyStatusSummary);
    }

    @Override
    public Response<Map<Integer, Double>> getMonthlyRevenueSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.calculateMonthlyRevenueForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, giá trị mặc định là 0.0
        Map<Integer, Double> monthlyRevenue = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyRevenue.put(month, 0.0);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Double revenue = (Double) result[1];
            monthlyRevenue.put(month, revenue);
        }

        return Response.successfulResponse("Lấy tổng doanh thu theo từng tháng thành công.", monthlyRevenue);
    }

    @Override
    public Response<Map<Integer, Double>> getMonthlyRepairCostSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.calculateMonthlyRepairCostForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, giá trị mặc định là 0.0
        Map<Integer, Double> monthlyRepairCostSummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyRepairCostSummary.put(month, 0.0);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Object repairCostObj = result[1];
            Double repairCost = repairCostObj instanceof Integer
                    ? ((Integer) repairCostObj).doubleValue()
                    : (Double) repairCostObj;
            monthlyRepairCostSummary.put(month, repairCost);
        }

        return Response.successfulResponse("Lấy tổng chi phí sửa chữa theo từng tháng thành công.", monthlyRepairCostSummary);
    }

    @Override
    public Response<Map<Integer, Double>> getMonthlyLateFeeSummary(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.calculateMonthlyLateFeeForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, giá trị mặc định là 0.0
        Map<Integer, Double> monthlyLateFeeSummary = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyLateFeeSummary.put(month, 0.0);
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Double lateFee = (Double) result[1];
            monthlyLateFeeSummary.put(month, lateFee);
        }

        return Response.successfulResponse("Lấy tổng số tiền phí trả xe muộn theo từng tháng thành công.", monthlyLateFeeSummary);
    }

    @Override
    public Response<List<Map<String, Object>>> getTopRevenueCars(Integer userId) {
        List<Object[]> results = bookingRepo.findTopRevenueCarsForOwner(userId);
        results = results.stream().limit(10).toList(); // Giới hạn kết qu�� chỉ lấy top 10
        // Chuyển đổi kết quả truy vấn thành danh sách Map
        List<Map<String, Object>> topRevenueCars = results.stream()
                .map(result -> Map.of(
                        "carId", result[0],
                        "carName", result[1],
                        "totalRevenue", result[2]
                ))
                .toList();

        return Response.successfulResponse("Lấy danh sách top 10 xe có doanh thu cao nhất thành công.", topRevenueCars);
    }

    @Override
    public Response<List<Map<String, Object>>> getTopRentedCars(Integer userId) {
        List<Object[]> results = bookingRepo.findTopRentedCarsForOwner(userId);

        // Giới hạn kết quả chỉ lấy Top 10
        results = results.stream().limit(10).toList();

        // Chuyển đổi kết quả truy vấn thành danh sách Map
        List<Map<String, Object>> topRentedCars = results.stream()
                .map(result -> Map.of(
                        "carId", result[0],
                        "carName", result[1],
                        "rentalCount", result[2]
                ))
                .toList();

        return Response.successfulResponse("Lấy danh sách Top 10 xe được thuê nhiều nhất thành công.", topRentedCars);
    }

    @Override
    public Response<Map<Integer, List<Map<String, Object>>>> getMonthlyTopRevenueCars(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.findMonthlyTopRevenueCarsForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, mỗi tháng là một danh sách xe
        Map<Integer, List<Map<String, Object>>> monthlyTopRevenueCars = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyTopRevenueCars.put(month, new ArrayList<>());
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Integer carId = (Integer) result[1];
            String carName = (String) result[2];
            Double totalRevenue = (Double) result[3];

            monthlyTopRevenueCars.get(month).add(Map.of(
                    "carId", carId,
                    "carName", carName,
                    "totalRevenue", totalRevenue
            ));
        }

        return Response.successfulResponse("Lấy danh sách top xe có doanh thu cao nhất theo từng tháng thành công.", monthlyTopRevenueCars);
    }

    @Override
    public Response<Map<Integer, List<Map<String, Object>>>> getMonthlyTopRentedCars(Integer userId) {
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> results = bookingRepo.findMonthlyTopRentedCarsForOwner(currentYear, userId);

        // Khởi tạo Map với 12 tháng, mỗi tháng là một danh sách xe
        Map<Integer, List<Map<String, Object>>> monthlyTopRentedCars = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyTopRentedCars.put(month, new ArrayList<>());
        }

        // Ghi đè giá trị cho các tháng có dữ liệu
        for (Object[] result : results) {
            Integer month = (Integer) result[0];
            Integer carId = (Integer) result[1];
            String carName = (String) result[2];
            Long rentalCount = (Long) result[3];

            monthlyTopRentedCars.get(month).add(Map.of(
                    "carId", carId,
                    "carName", carName,
                    "rentalCount", rentalCount
            ));
        }

        return Response.successfulResponse("Lấy danh sách top xe được thuê nhiều nhất theo từng tháng thành công.", monthlyTopRentedCars);
    }


}
