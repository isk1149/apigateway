package com.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.logging.Handler;

@Component
@Slf4j
public class CustomFilter extends AbstractGatewayFilterFactory<CustomFilter.Config> {

    public CustomFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        //custom pre filter. suppose we can extract jwt and perform authentication.
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders newHeaders = new HttpHeaders();
            newHeaders.addAll(request.getHeaders());
            newHeaders.add("first-request", "first-request-header-value");

            ServerHttpRequest newRequest = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() {
                    return newHeaders;
                }
            };
            ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();

            log.info("custom pre filter: request uri -> {}", newRequest.getId());
            //custom post filter. suppose we can call error response handler based on error code.
            return chain.filter(newExchange).then(Mono.fromRunnable(() -> {
                log.info("custom post filter: response code -> {}", response.getStatusCode());
            }));
        };
    }

    public static class Config {

    }
}
