package com.chuseok22.umbrellareturn.client;

import com.chuseok22.umbrellareturn.config.AligoProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class AligoSmsClient {

    private static final String ALIGO_API_URL = "https://apis.aligo.in/send/";

    private final AligoProperties aligoProperties;
    private final RestClient restClient;

    public AligoSmsClient(AligoProperties aligoProperties) {
        this.aligoProperties = aligoProperties;
        this.restClient = RestClient.create();
    }

    public boolean send(String receiver, String message) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("key", aligoProperties.getApiKey());
        params.add("user_id", aligoProperties.getUserId());
        params.add("sender", aligoProperties.getSender());
        params.add("receiver", receiver);
        params.add("msg", message);
        params.add("msg_type", "SMS");

        try {
            String response = restClient.post()
                .uri(ALIGO_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(String.class);

            return response != null && response.contains("\"result_code\":\"1\"");
        } catch (Exception e) {
            return false;
        }
    }
}
