package com.chuseok22.umbrellareturn.scheduler;

import com.chuseok22.umbrellareturn.service.SmsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SmsScheduler {

    private final SmsService smsService;

    public SmsScheduler(SmsService smsService) {
        this.smsService = smsService;
    }

    // 매일 오전 7시에 미반납 우산 보유자에게 SMS 발송
    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyReminder() {
        smsService.sendReminderToAll();
    }
}
