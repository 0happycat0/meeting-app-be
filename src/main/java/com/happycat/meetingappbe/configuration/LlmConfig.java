package com.happycat.meetingappbe.configuration;

import io.netty.channel.ChannelOption;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;

@Getter
@Setter
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LlmConfig {
    @Value("${llm.base-url}")
    String baseUrl;

    @Value("${llm.chat-completions-path}")
    String chatCompletionsPath;

    @Value("${llm.model}")
    String model;

    @Value("${llm.timeout-seconds}")
    long timeoutSeconds;

    @Value("${llm.proxy.enabled}")
    boolean proxyEnabled;

    @Value("${llm.proxy.host}")
    String proxyHost;

    @Value("${llm.proxy.port}")
    int proxyPort;

    @Bean
    @Qualifier("llmWebClient")
    WebClient llmWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        if (proxyEnabled) {
            httpClient = httpClient.proxy(proxy -> configureProxy(proxy));
        }

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private void configureProxy(ProxyProvider.TypeSpec proxy) {
        if (proxyHost == null || proxyHost.isBlank() || proxyPort <= 0) {
            throw new IllegalStateException("LLM proxy is enabled but host/port is not configured");
        }
        proxy.type(ProxyProvider.Proxy.HTTP)
                .host(proxyHost)
                .port(proxyPort);
    }
}
