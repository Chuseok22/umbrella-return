package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.entity.Umbrella;
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
class UmbrellaServiceTest {

    @Mock UmbrellaRepository umbrellaRepository;
    @InjectMocks UmbrellaService umbrellaService;

    @Test
    void 중복_번호_등록_시_예외() {
        given(umbrellaRepository.existsByNumber("001")).willReturn(true);

        assertThatThrownBy(() -> umbrellaService.register("001"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 등록된");
    }

    @Test
    void 정상_등록() {
        given(umbrellaRepository.existsByNumber("002")).willReturn(false);
        given(umbrellaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> umbrellaService.register("002")).doesNotThrowAnyException();
        then(umbrellaRepository).should().save(any(Umbrella.class));
    }

    @Test
    void 대여중_우산_삭제_시_예외() {
        Umbrella rented = new Umbrella("003");
        rented.markRented();
        given(umbrellaRepository.findById(1L)).willReturn(Optional.of(rented));

        assertThatThrownBy(() -> umbrellaService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("대여 중인 우산");
    }
}
