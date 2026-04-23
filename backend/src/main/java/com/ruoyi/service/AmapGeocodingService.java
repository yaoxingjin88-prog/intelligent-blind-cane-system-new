package com.ruoyi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.config.AmapConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmapGeocodingService {

    private final AmapConfig amapConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        if (amapConfig.getWebKey() == null || amapConfig.getWebKey().isBlank()) {
            return null;
        }
        try {
            String location = longitude + "," + latitude;
            String url = amapConfig.getBaseUrl()
                    + "/v3/geocode/regeo?key=" + URLEncoder.encode(amapConfig.getWebKey(), StandardCharsets.UTF_8)
                    + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                    + "&extensions=base&batch=false&roadlevel=0";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(amapConfig.getTimeoutSeconds()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(amapConfig.getTimeoutSeconds()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                log.warn("高德逆地理编码失败: status={}", response.statusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"1".equals(root.path("status").asText())) {
                log.warn("高德逆地理编码返回异常: info={}, infocode={}", root.path("info").asText(), root.path("infocode").asText());
                return null;
            }

            JsonNode regeocode = root.path("regeocode");
            String formattedAddress = regeocode.path("formatted_address").asText("").trim();
            if (!formattedAddress.isEmpty()) {
                return formattedAddress;
            }

            JsonNode addressComponent = regeocode.path("addressComponent");
            String province = addressComponent.path("province").asText("");
            String city = addressComponent.path("city").isArray()
                    ? ""
                    : addressComponent.path("city").asText("");
            String district = addressComponent.path("district").asText("");
            String township = addressComponent.path("township").asText("");
            StringBuilder fallback = new StringBuilder();
            fallback.append(province).append(city).append(district).append(township);
            String text = fallback.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.warn("高德逆地理编码异常", e);
            return null;
        }
    }
}
