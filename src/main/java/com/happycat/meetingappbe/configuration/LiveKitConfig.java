package com.happycat.meetingappbe.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LiveKitConfig {
    @Value("${livekit.api-url}")
    String apiUrl;

    @Value("${livekit.ws-url}")
    String wsUrl;

    @Value("${livekit.api-key}")
    String apiKey;

    @Value("${livekit.api-secret}")
    String apiSecret;

    @Value("${livekit.token-ttl-seconds}")
    long tokenTtlSeconds;
}
