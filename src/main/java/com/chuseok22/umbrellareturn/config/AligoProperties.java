package com.chuseok22.umbrellareturn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aligo")
public class AligoProperties {

    private String apiKey;
    private String userId;
    private String sender;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
}
