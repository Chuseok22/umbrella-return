package com.chuseok22.umbrellareturn.client;

import com.chuseok22.umbrellareturn.config.CoolSmsProperties;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CoolSmsClient {

    private static final Logger log = LoggerFactory.getLogger(CoolSmsClient.class);

    private final CoolSmsProperties coolSmsProperties;
    private final DefaultMessageService messageService;

    public CoolSmsClient(CoolSmsProperties coolSmsProperties) {
        this.coolSmsProperties = coolSmsProperties;
        // apiKey/apiSecret 미설정 시 null로 유지 (테스트 환경 대비)
        if (coolSmsProperties.getApiKey() != null && coolSmsProperties.getApiSecret() != null) {
            this.messageService = NurigoApp.INSTANCE.initialize(
                coolSmsProperties.getApiKey(),
                coolSmsProperties.getApiSecret(),
                "https://api.coolsms.co.kr"
            );
        } else {
            this.messageService = null;
        }
    }

    /**
     * CoolSMS API로 SMS를 발송하고 응답 정보 문자열을 반환한다.
     * 발송 실패(예외 발생) 또는 미설정 시 null을 반환한다.
     */
    public String send(String receiver, String message) {
        if (messageService == null) {
            log.warn("CoolSMS 미설정: coolsms.api-key, coolsms.api-secret 환경변수를 확인하세요.");
            return null;
        }

        Message msg = new Message();
        msg.setFrom(coolSmsProperties.getSender());
        msg.setTo(receiver);
        msg.setText(message);

        try {
            SingleMessageSentResponse response = messageService.sendOne(
                new SingleMessageSendingRequest(msg)
            );
            return "statusCode=" + response.getStatusCode() + ",messageId=" + response.getMessageId();
        } catch (Exception e) {
            log.warn("CoolSMS 발송 실패: receiver={}, error={}", receiver, e.getMessage());
            return null;
        }
    }

    // CoolSMS SDK는 실패 시 예외를 throw하므로 null 체크만으로 성공 여부를 판단한다
    public static boolean isSuccess(String response) {
        return response != null;
    }
}
