package com.artcorb.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.artcorb.gatewayserver.util.FilterUtil;
import reactor.core.publisher.Mono;

@Order(1)
@Component
public class RequestTraceFilter implements GlobalFilter {

  private static final Logger logger = LoggerFactory.getLogger(RequestTraceFilter.class);

  @Autowired
  FilterUtil filterUtil;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
    if (isCorrelationIdPresent(requestHeaders)) {
      logger.debug(FilterUtil.CORRELATION_ID + " found in RequestTraceFilter : {}",
          filterUtil.getCorrelationId(requestHeaders));
    } else {
      String correlationID = generateCorrelationId();
      exchange = filterUtil.setCorrelationId(exchange, correlationID);
      logger.debug(FilterUtil.CORRELATION_ID + " generated in RequestTraceFilter : {}",
          correlationID);
    }

    return chain.filter(exchange);
  }

  private boolean isCorrelationIdPresent(HttpHeaders requestHeaders) {
    return filterUtil.getCorrelationId(requestHeaders) != null;
  }

  private String generateCorrelationId() {
    return java.util.UUID.randomUUID().toString();
  }

}
