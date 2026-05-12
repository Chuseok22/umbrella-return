package com.chuseok22.umbrellareturn.repository;

import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByStatusOrderByRentedAtDesc(RentalStatus status);

    List<Rental> findAllByOrderByRentedAtDesc();

    Optional<Rental> findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
        String umbrellaNumber, String borrowerName, String borrowerPhone, RentalStatus status
    );

    @Query("SELECT r FROM Rental r JOIN FETCH r.umbrella WHERE r.status = 'RENTED' AND r.rentedAt < :todayStart")
    List<Rental> findUnreturnedBefore(@Param("todayStart") LocalDateTime todayStart);

    long countByStatus(RentalStatus status);
}
