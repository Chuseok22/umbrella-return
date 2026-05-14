package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.client.CoolSmsClient;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.SmsLog;
import com.chuseok22.umbrellareturn.repository.SmsLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SmsService {

    private final CoolSmsClient coolSmsClient;
    private final SmsLogRepository smsLogRepository;
    private final RentalService rentalService;

    public SmsService(CoolSmsClient coolSmsClient, SmsLogRepository smsLogRepository, RentalService rentalService) {
        this.coolSmsClient = coolSmsClient;
        this.smsLogRepository = smsLogRepository;
        this.rentalService = rentalService;
    }

    @Transactional
    public int sendReminderToAll() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Rental> unreturned = rentalService.findUnreturnedBefore(todayStart);

        int successCount = 0;
        for (Rental rental : unreturned) {
            String message = buildMessage(rental);
            String response = coolSmsClient.send(rental.getBorrowerPhone(), message);
            boolean success = CoolSmsClient.isSuccess(response);
            smsLogRepository.save(new SmsLog(rental, rental.getBorrowerPhone(), message, success, response));
            if (success) successCount++;
        }
        return successCount;
    }

    public List<SmsLog> findAllLogs() {
        return smsLogRepository.findAllByOrderBySentAtDesc();
    }

    private String buildMessage(Rental rental) {
        return String.format(
            "[인터페이스] 우산 반납 알림\n%s님, 대여하신 %s번 우산을\n아직 반납하지 않으셨습니다.\n우산함 앞 QR코드로 반납 부탁드립니다.",
            rental.getBorrowerName(),
            rental.getUmbrella().getNumber()
        );
    }
}
