package com.artcorb.gatewayserver;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;

@SpringBootApplication
public class GatewayserverApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayserverApplication.class, args);
  }

  /**
   * Config to register paths (routes), and put filters on them. For example:
   * 
   * - Connect to the discovery server (eureka) and forward the trafic from external clients to the
   * microservices. It is a good practice create a URL and point it to a Eureka URI.
   * 
   * - Add a response header with the LocalDateTime.now(), for performance measure.
   */
  @Bean
  RouteLocator routeConfig(RouteLocatorBuilder routeLocatorBuilder) {
    return routeLocatorBuilder.routes()
        .route(p -> p.path("/sbdb/accounts/**")
            .filters(f -> f.rewritePath("/sbdb/accounts/(?<segment>.*)", "/${segment}")
                .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                .circuitBreaker(cfg -> cfg.setName("accountsCircuitBreaker")
                    .setFallbackUri("forward:/contactSupport")))
            .uri("lb://ACCOUNTS"))

        .route(
            p -> p.path("/sbdb/loans/**")
                .filters(f -> f.rewritePath("/sbdb/loans/(?<segment>.*)", "/${segment}")
                    .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                    .retry(cfg -> {
                      cfg.setRetries(3)
                          // Enable retry pattern only for get operations. It is a good practice to
                          // enable only for GET methods, because this behavior won't be any side
                          // effects (like insert two times the same register in POST methods, for
                          // example).
                          .setMethods(HttpMethod.GET)
                          // Set the retry interval.
                          .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true);
                    }))
                .uri("lb://LOANS"))

        .route(p -> p.path("/sbdb/cards/**")
            .filters(f -> f.rewritePath("/sbdb/cards/(?<segment>.*)", "/${segment}")
                .addResponseHeader("X-Response-Time", LocalDateTime.now().toString()))
            .uri("lb://CARDS"))

        .build();
  }

}
