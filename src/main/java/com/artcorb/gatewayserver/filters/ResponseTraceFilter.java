package com.artcorb.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import com.artcorb.gatewayserver.util.FilterUtil;
import reactor.core.publisher.Mono;

@Configuration
public class ResponseTraceFilter {

  private static final Logger logger = LoggerFactory.getLogger(ResponseTraceFilter.class);

  @Autowired
  FilterUtil filterUtil;

  @Bean
  GlobalFilter postGlobalFilter() {
    return (exchange, chain) -> {
      return chain.filter(exchange).then(Mono.fromRunnable(() -> {
        if (exchange.getResponse().getHeaders().containsKey(FilterUtil.CORRELATION_ID))
          return;

        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        String correlationId = filterUtil.getCorrelationId(requestHeaders);

        logger.debug("Updated the correlation id to the outbound headers: {}", correlationId);

        exchange.getResponse().getHeaders().add(FilterUtil.CORRELATION_ID, correlationId);
      }));
    };
  }

}
