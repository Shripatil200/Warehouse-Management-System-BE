package com.infotact.warehouse.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@Slf4j
@Component
public class SmsUtils {


    // Ensure this is the full key from your 'API Key' tab
    @Value("${app.sms.api.key}")
    private  String AUTH_KEY;
    private final String BASE_URL = "https://www.fast2sms.com/dev/bulkV2";
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtpSms(String contactNumber, String otp) {
        try {
            // 1. Clean number (10 digits only)
            String cleanedNumber = contactNumber.replaceAll("[^0-9]", "");
            if (cleanedNumber.length() > 10) {
                cleanedNumber = cleanedNumber.substring(cleanedNumber.length() - 10);
            }

            // 2. Format the message for Quick SMS
            String smsMessage = "Your Warehouse System verification code is: " + otp;

            // 3. Build URI for 'route=q'
            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("authorization", AUTH_KEY)
                    .queryParam("route", "q")               // Quick Route
                    .queryParam("message", smsMessage)      // Full text message
                    .queryParam("language", "english")
                    .queryParam("flash", "0")
                    .queryParam("numbers", cleanedNumber)
                    .build()
                    .toUri();

            log.info("Triggering Quick SMS to: {}", cleanedNumber);

            String response = restTemplate.getForObject(uri, String.class);
            log.info("Fast2SMS Quick Response: {}", response);

            if (response == null || !response.contains("\"return\":true")) {
                throw new RuntimeException("SMS Delivery Failed: " + response);
            }

        } catch (Exception e) {
            log.error("Quick SMS API Error: {}", e.getMessage());
            // Fail silently or throw based on your UI preference
            throw new RuntimeException("SMS Service is currently unavailable.");
        }
    }
}