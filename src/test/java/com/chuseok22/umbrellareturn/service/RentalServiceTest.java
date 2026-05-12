package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.repository.RentalRepository;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock RentalRepository rentalRepository;
    @Mock UmbrellaRepository umbrellaRepository;
    @InjectMocks RentalService rentalService;

    private RentForm rentForm(String umbrellaNumber) {
        RentForm form = new RentForm();
        form.setUmbrellaNumber(umbrellaNumber);
        form.setBorrowerName("홍길동");
        form.setBorrowerPhone("01012345678");
        form.setBorrowerStudentId("20230001");
        return form;
    }

    @Test
    void 이미_대여중인_우산_대여_시_예외() {
        Umbrella rented = new Umbrella("001");
        rented.markRented();
        given(umbrellaRepository.findByNumber("001")).willReturn(Optional.of(rented));

        assertThatThrownBy(() -> rentalService.rent(rentForm("001")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 대여 중");
    }

    @Test
    void 정상_대여() {
        Umbrella available = new Umbrella("002");
        given(umbrellaRepository.findByNumber("002")).willReturn(Optional.of(available));
        given(rentalRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> rentalService.rent(rentForm("002"))).doesNotThrowAnyException();
        then(rentalRepository).should().save(any(Rental.class));
    }

    @Test
    void 반납_정보_불일치_시_예외() {
        ReturnForm form = new ReturnForm();
        form.setUmbrellaNumber("001");
        form.setBorrowerName("홍길동");
        form.setBorrowerPhone("01099999999");

        given(rentalRepository.findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
            "001", "홍길동", "01099999999", RentalStatus.RENTED
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.returnUmbrella(form))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("일치하지 않습니다");
    }
}
