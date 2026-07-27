package com.athena.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "athena.sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {

    private static final String BASE_URL = "https://api.twilio.com/2010-04-01";

    private final SmsProperties properties;
    private final RestClient restClient;

    public TwilioSmsSender(SmsProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeaders(headers -> headers.setBasicAuth(
                        properties.getTwilio().getAccountSid(),
                        properties.getTwilio().getAuthToken()))
                .build();
    }

    @Override
    public void send(String phoneNumber, String message) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phoneNumber);
        form.add("From", properties.getTwilio().getFromNumber());
        form.add("Body", message);

        restClient.post()
                .uri("/Accounts/{sid}/Messages.json", properties.getTwilio().getAccountSid())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        log.info("Sent SMS via Twilio to {}", phoneNumber);
    }
}
