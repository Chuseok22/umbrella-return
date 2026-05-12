package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.repository.RentalRepository;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UmbrellaRepository umbrellaRepository;

    public RentalService(RentalRepository rentalRepository, UmbrellaRepository umbrellaRepository) {
        this.rentalRepository = rentalRepository;
        this.umbrellaRepository = umbrellaRepository;
    }

    @Transactional
    public void rent(RentForm form) {
        Umbrella umbrella = umbrellaRepository.findByNumber(form.getUmbrellaNumber())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 우산 번호입니다."));

        if (!umbrella.isAvailable()) {
            throw new IllegalStateException("이미 대여 중인 우산입니다.");
        }

        umbrella.markRented();
        rentalRepository.save(new Rental(
            umbrella,
            form.getBorrowerName(),
            form.getBorrowerPhone(),
            form.getBorrowerStudentId()
        ));
    }

    @Transactional
    public void returnUmbrella(ReturnForm form) {
        Rental rental = rentalRepository.findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
            form.getUmbrellaNumber(), form.getBorrowerName(), form.getBorrowerPhone(), RentalStatus.RENTED
        ).orElseThrow(() -> new IllegalArgumentException("대여 정보가 일치하지 않습니다."));

        rental.markReturned();
        rental.getUmbrella().markAvailable();
    }

    @Transactional
    public void adminReturn(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 내역입니다."));
        rental.markReturned();
        rental.getUmbrella().markAvailable();
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
