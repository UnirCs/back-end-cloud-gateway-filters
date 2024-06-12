package com.unir.gateway.decorator;

import java.net.URI;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unir.gateway.model.GatewayRequest;

import lombok.NonNull;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;

public class PatchRequestDecorator extends ServerHttpRequestDecorator {

    private final GatewayRequest gatewayRequest;
    private final ObjectMapper objectMapper;

    public PatchRequestDecorator(GatewayRequest gatewayRequest, ObjectMapper objectMapper) {
        super(gatewayRequest.getExchange().getRequest());
        this.gatewayRequest = gatewayRequest;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("null")
    @Override
    @NonNull
    public HttpMethod getMethod() {
        return HttpMethod.PATCH;
    }

    @SuppressWarnings("null")
    @Override
    @NonNull
    public URI getURI() {
        return UriComponentsBuilder.fromUri((URI) gatewayRequest.getExchange().getAttributes().get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
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
    @SneakyThrows
    public Flux<DataBuffer> getBody() {
        DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        byte[] bodyData = objectMapper.writeValueAsBytes(gatewayRequest.getBody());
        DataBuffer buffer = bufferFactory.allocateBuffer(bodyData.length);
        buffer.write(bodyData);
        return Flux.just(buffer);
    }
}
