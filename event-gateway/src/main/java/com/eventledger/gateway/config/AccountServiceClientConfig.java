package com.eventledger.gateway.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AccountServiceClientConfig {

    @Bean
    public RestClient accountServiceRestClient(
            @Value("${account-service.base-url}") String baseUrl,
            @Value("${account-service.connect-timeout}") Duration connectTimeout,
            @Value("${account-service.read-timeout}") Duration readTimeout,
            Tracer tracer) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    Span current = tracer.currentSpan();
                    if (current != null) {
                        TraceContext ctx = current.context();
                        String flags = Boolean.TRUE.equals(ctx.sampled()) ? "01" : "00";
                        request.getHeaders().set("traceparent",
                                "00-" + ctx.traceId() + "-" + ctx.spanId() + "-" + flags);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
