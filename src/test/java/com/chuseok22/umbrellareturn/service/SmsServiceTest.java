package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.client.CoolSmsClient;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.repository.SmsLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock CoolSmsClient coolSmsClient;
    @Mock SmsLogRepository smsLogRepository;
    @Mock RentalService rentalService;
    @InjectMocks SmsService smsService;

    @Test
    void 미반납자_전체_발송_성공_카운트() {
        Umbrella umbrella = new Umbrella("001");
        Rental rental = new Rental(umbrella, "홍길동", "01012345678", "20230001");

        given(rentalService.findUnreturnedBefore(any(LocalDateTime.class)))
            .willReturn(List.of(rental));
        given(coolSmsClient.send(anyString(), anyString())).willReturn("statusCode=2000,messageId=abc123");
        given(smsLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        int count = smsService.sendReminderToAll();

        assertThat(count).isEqualTo(1);
        then(smsLogRepository).should().save(any());
    }

    @Test
    void 미반납자_없으면_발송_안함() {
        given(rentalService.findUnreturnedBefore(any(LocalDateTime.class)))
            .willReturn(List.of());

        int count = smsService.sendReminderToAll();

        assertThat(count).isEqualTo(0);
        then(coolSmsClient).shouldHaveNoInteractions();
    }
}
