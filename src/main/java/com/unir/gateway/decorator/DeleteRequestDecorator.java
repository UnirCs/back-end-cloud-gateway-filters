package com.unir.gateway.decorator;

import java.net.URI;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.util.UriComponentsBuilder;

import com.unir.gateway.model.GatewayRequest;

import lombok.NonNull;
import reactor.core.publisher.Flux;

public class DeleteRequestDecorator extends ServerHttpRequestDecorator {

    private final GatewayRequest gatewayRequest;

    public DeleteRequestDecorator(GatewayRequest gatewayRequest) {
        super(gatewayRequest.getExchange().getRequest());
        this.gatewayRequest = gatewayRequest;
    }

    @SuppressWarnings("null")
    @Override
    @NonNull
    public HttpMethod getMethod() {
        return HttpMethod.DELETE;
    }

    @SuppressWarnings("null")
    @Override
    @NonNull
    public URI getURI() {
        return UriComponentsBuilder
                .fromUri((URI) gatewayRequest.getExchange().getAttributes().get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
                .build()
                .toUri();
    }

    @SuppressWarnings("null")
    @Override
    @NonNull
    public HttpHeaders getHeaders() {
        return gatewayRequest.getHeaders();
    }

    @SuppressWarnings("null")
    @Override
    @NonNull
    public Flux<DataBuffer> getBody() {
        return Flux.empty();
    }
}
