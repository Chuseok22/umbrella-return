package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.exception.CustomException;
import com.chuseok22.umbrellareturn.exception.ErrorCode;
import com.chuseok22.umbrellareturn.repository.RentalRepository;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RentalService {

    private static final Logger log = LoggerFactory.getLogger(RentalService.class);

    private final RentalRepository rentalRepository;
    private final UmbrellaRepository umbrellaRepository;

    public RentalService(RentalRepository rentalRepository, UmbrellaRepository umbrellaRepository) {
        this.rentalRepository = rentalRepository;
        this.umbrellaRepository = umbrellaRepository;
    }

    @Transactional
    public void rent(RentForm form) {
        Umbrella umbrella = umbrellaRepository.findByNumber(form.getUmbrellaNumber())
            .orElseThrow(() -> new CustomException(ErrorCode.UMBRELLA_NOT_FOUND));

        if (!umbrella.isAvailable()) {
            throw new CustomException(ErrorCode.UMBRELLA_NOT_AVAILABLE);
        }

        umbrella.markRented();
        rentalRepository.save(new Rental(
            umbrella,
            form.getBorrowerName(),
            form.getBorrowerPhone(),
            form.getBorrowerStudentId()
        ));
        log.info("우산 대여 완료: umbrella={}, borrower={}", form.getUmbrellaNumber(), form.getBorrowerName());
    }

    @Transactional
    public void returnUmbrella(ReturnForm form) {
        Rental rental = rentalRepository.findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
            form.getUmbrellaNumber(), form.getBorrowerName(), form.getBorrowerPhone(), RentalStatus.RENTED
        ).orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));

        rental.markReturned();
        rental.getUmbrella().markAvailable();
        log.info("우산 반납 완료: umbrella={}, borrower={}", form.getUmbrellaNumber(), form.getBorrowerName());
    }

    @Transactional
    public void adminReturn(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_RECORD_NOT_FOUND));
        rental.markReturned();
        rental.getUmbrella().markAvailable();
        log.info("관리자 반납 처리 완료: rentalId={}", rentalId);
    }

    public List<Rental> findActiveRentals() {
        return rentalRepository.findByStatusOrderByRentedAtDesc(RentalStatus.RENTED);
    }

    public List<Rental> findAllRentals() {
        return rentalRepository.findAllByOrderByRentedAtDesc();
    }

    public List<Rental> findUnreturnedBefore(LocalDateTime todayStart) {
        return rentalRepository.findUnreturnedBefore(RentalStatus.RENTED, todayStart);
    }

    public long countActive() {
        return rentalRepository.countByStatus(RentalStatus.RENTED);
    }
}
