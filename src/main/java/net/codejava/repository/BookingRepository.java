package net.codejava.repository;

import net.codejava.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import net.codejava.domain.entity.Booking;
import net.codejava.domain.enums.BookingStatus;
import java.time.LocalDateTime;

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

    @Query("SELECT b FROM Booking b WHERE b.car.carOwner.id = :ownerId AND " +
            "(LOWER(b.car.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Booking> getListBookingByOwnerIdWithKeyword(@Param("ownerId") Integer ownerId,
                                                     @Param("keyword") String keyword,
                                                     Pageable pageable);


    @Query("SELECT DISTINCT b.user FROM Booking b WHERE b.car.carOwner.id = :ownerId")
    Page<User> getListCustomerByOwnerId(@Param("ownerId") Integer ownerId, Pageable pageable);

    @Query("SELECT DISTINCT b.user FROM Booking b WHERE b.car.carOwner.id = :ownerId AND " +
            "(LOWER(b.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> getListCustomerByOwnerIdWithKeyword(@Param("ownerId") Integer ownerId,
                                                   @Param("keyword") String keyword,
                                                   Pageable pageable);

    @Query("SELECT b.user, COUNT(b) FROM Booking b WHERE b.car.carOwner.id = :ownerId AND (:userName IS NULL OR LOWER(b.user.username) LIKE LOWER(CONCAT('%', :userName, '%')))  GROUP BY b.user")
    List<Object[]> getListCustomerWithBookingCountByOwnerId(@Param("ownerId") Integer ownerId,@Param("userName") String userName, Pageable pageable);

    List<Booking> findAllByStatusAndStartDateTimeBefore(BookingStatus status, LocalDateTime time);
    List<Booking> findAllByStatus(BookingStatus status);
}
