package net.codejava.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.codejava.domain.entity.Booking;
import net.codejava.domain.entity.User;
import net.codejava.domain.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    @Query("SELECT b FROM Booking b " + "JOIN FETCH b.car c " + "JOIN FETCH b.user u " + "WHERE u.id = :userId")
    Page<Booking> getListBookingByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT b FROM Booking b " + "JOIN FETCH b.car c " + "JOIN FETCH b.user u " + "WHERE c.id = :carId")
    Page<Booking> getListBookingByCarId(@Param("carId") Integer carId, Pageable pageable);

    @Query("SELECT b FROM Booking b " + "JOIN FETCH b.car c "
            + "JOIN FETCH b.user u "
            + "WHERE u.id = :userId AND CONCAT(b.status,' ',b.paymentMethod) LIKE %:keyword%")
    Page<Booking> getListBookingByUserIdWithKeyword(
            @Param("userId") Integer userId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.car.carOwner.id = :ownerId")
    Page<Booking> getListBookingByOwnerId(@Param("ownerId") Integer ownerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.car.carOwner.id = :ownerId AND "
            + "(LOWER(b.car.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
            + "LOWER(b.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Booking> getListBookingByOwnerIdWithKeyword(
            @Param("ownerId") Integer ownerId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT b.user FROM Booking b WHERE b.car.carOwner.id = :ownerId")
    Page<User> getListCustomerByOwnerId(@Param("ownerId") Integer ownerId, Pageable pageable);

    @Query("SELECT DISTINCT b.user FROM Booking b WHERE b.car.carOwner.id = :ownerId AND "
            + "(LOWER(b.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
            + "LOWER(b.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> getListCustomerByOwnerIdWithKeyword(
            @Param("ownerId") Integer ownerId, @Param("keyword") String keyword, Pageable pageable);

    @Query(
            "SELECT b.user, COUNT(b) FROM Booking b WHERE b.car.carOwner.id = :ownerId AND (:userName IS NULL OR LOWER(b.user.username) LIKE LOWER(CONCAT('%', :userName, '%')))  GROUP BY b.user")
    List<Object[]> getListCustomerWithBookingCountByOwnerId(
            @Param("ownerId") Integer ownerId, @Param("userName") String userName, Pageable pageable);

    @Query("SELECT u, COALESCE(COUNT(b), 0) FROM User u " + "LEFT JOIN Booking b ON b.user = u "
            + "WHERE (:userName IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :userName, '%'))) "
            + "GROUP BY u")
    List<Object[]> getListCustomer(@Param("userName") String userName, Pageable pageable);

    List<Booking> findAllByStatusAndStartDateTimeBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findAllByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.car.carOwner.id = :userId "
            + "AND (:bookingStatus IS NULL OR b.status = :bookingStatus) "
            + "AND (:carName IS NULL OR b.car.name LIKE %:carName%) "
            + "AND (:startDateTime IS NULL OR b.startDateTime >= :startDateTime) "
            + "AND (:endDateTime IS NULL OR b.endDateTime <= :endDateTime)")
    Page<Booking> findBookingsByFilter(
            @Param("userId") Integer userId,
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("carName") String carName,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE 1=1 "
            + "AND (:bookingStatus IS NULL OR b.status = :bookingStatus) "
            + "AND (:carName IS NULL OR b.car.name LIKE %:carName%) "
            + "AND (:startDateTime IS NULL OR b.startDateTime >= :startDateTime) "
            + "AND (:endDateTime IS NULL OR b.endDateTime <= :endDateTime)")
    Page<Booking> findAllBookingsByFilter(
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("carName") String carName,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(b) AS totalBookings " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countBookingsByMonth(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(b) AS count " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.user.id = :userId "
            + "GROUP BY MONTH(b.startDateTime)")
    List<Object[]> countBookingsByMonthForUser(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(DISTINCT b.car.id) AS totalProducts " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countProductsByMonthForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(DISTINCT b.user.id) AS totalCustomers " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countCustomersByMonthForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query(
            "SELECT MONTH(b.startDateTime) AS month, SUM(TIMESTAMPDIFF(HOUR, b.startDateTime, b.endDateTime)) AS totalHours "
                    + "FROM Booking b "
                    + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
                    + "GROUP BY MONTH(b.startDateTime) "
                    + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countHoursByMonthForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, b.status AS status, COUNT(b) AS count " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime), b.status "
            + "ORDER BY MONTH(b.startDateTime), b.status")
    List<Object[]> countBookingsByStatusAndMonthForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT b.user, COUNT(b) " + "FROM Booking b "
            + "WHERE b.car.carOwner.id = :ownerId AND (b.status = 'PICK_UP' OR b.status = 'CONFIRM') "
            + "GROUP BY b.user "
            + "ORDER BY COUNT(b) DESC")
    List<Object[]> getListCustomerWithPickupOrConfirmOrders(@Param("ownerId") Integer ownerId, Pageable pageable);

    @Query(
            "SELECT MONTH(b.startDateTime) AS month, SUM(COALESCE(b.rental_amount, 0) + COALESCE(b.extraFee, 0)) AS totalRevenue "
                    + "FROM Booking b "
                    + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
                    + "GROUP BY MONTH(b.startDateTime) "
                    + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> calculateMonthlyRevenueForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query(
            "SELECT MONTH(b.startDateTime) AS month, b.car.id AS carId, SUM(COALESCE(b.compensationFee, 0)) AS totalRepairCost "
                    + "FROM Booking b "
                    + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
                    + "GROUP BY MONTH(b.startDateTime), b.car.id "
                    + "ORDER BY MONTH(b.startDateTime), b.car.id")
    List<Object[]> calculateMonthlyRepairCostForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, SUM(COALESCE(b.extraFee, 0)) AS totalLateFee " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> calculateMonthlyLateFeeForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query(
            "SELECT b.car.id AS carId, b.car.name AS carName, SUM(COALESCE(b.rental_amount, 0) + COALESCE(b.extraFee, 0)) AS totalRevenue "
                    + "FROM Booking b "
                    + "WHERE b.car.carOwner.id = :userId "
                    + "GROUP BY b.car.id, b.car.name "
                    + "ORDER BY totalRevenue DESC")
    List<Object[]> findTopRevenueCarsForOwner(@Param("userId") Integer userId);

    @Query("SELECT b.car.id AS carId, b.car.name AS carName, COUNT(b) AS rentalCount " + "FROM Booking b "
            + "WHERE b.car.carOwner.id = :userId "
            + "GROUP BY b.car.id, b.car.name "
            + "ORDER BY rentalCount DESC")
    List<Object[]> findTopRentedCarsForOwner(@Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, b.car.id AS carId, b.car.name AS carName, "
            + "SUM(COALESCE(b.rental_amount, 0) + COALESCE(b.extraFee, 0)) AS totalRevenue "
            + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime), b.car.id, b.car.name "
            + "ORDER BY MONTH(b.startDateTime), totalRevenue DESC")
    List<Object[]> findMonthlyTopRevenueCarsForOwner(@Param("year") int year, @Param("userId") Integer userId);

    @Query("SELECT MONTH(b.startDateTime) AS month, b.car.id AS carId, b.car.name AS carName, COUNT(b) AS rentalCount "
            + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year AND b.car.carOwner.id = :userId "
            + "GROUP BY MONTH(b.startDateTime), b.car.id, b.car.name "
            + "ORDER BY MONTH(b.startDateTime), rentalCount DESC")
    List<Object[]> findMonthlyTopRentedCarsForOwner(@Param("year") int year, @Param("userId") Integer userId);

    boolean existsByUserAndStatusIn(User user, List<String> statuses);

    // Admin methods - chỉ copy query user và bỏ điều kiện userId
    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(b) AS totalBookings " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countAllBookingsByMonth(@Param("year") int year);

    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(DISTINCT b.car.id) AS totalProducts " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countAllProductsByMonth(@Param("year") int year);

    @Query("SELECT MONTH(b.startDateTime) AS month, COUNT(DISTINCT b.user.id) AS totalCustomers " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year "
            + "GROUP BY MONTH(b.startDateTime) "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countAllCustomersByMonth(@Param("year") int year);

    @Query(
            "SELECT MONTH(b.startDateTime) AS month, SUM(TIMESTAMPDIFF(HOUR, b.startDateTime, b.endDateTime)) AS totalHours "
                    + "FROM Booking b "
                    + "WHERE YEAR(b.startDateTime) = :year "
                    + "GROUP BY MONTH(b.startDateTime) "
                    + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> countAllHoursByMonth(@Param("year") int year);

    @Query("SELECT MONTH(b.startDateTime) AS month, b.status AS status, COUNT(b) AS count " + "FROM Booking b "
            + "WHERE YEAR(b.startDateTime) = :year "
            + "GROUP BY MONTH(b.startDateTime), b.status "
            + "ORDER BY MONTH(b.startDateTime)")
    List<Object[]> getAllMonthlyStatusSummary(@Param("year") int year);
}
