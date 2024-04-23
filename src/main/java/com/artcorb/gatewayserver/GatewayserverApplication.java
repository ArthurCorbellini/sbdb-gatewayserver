package com.artcorb.gatewayserver;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import reactor.core.publisher.Mono;

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
                .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                .requestRateLimiter(cfg -> cfg.setRateLimiter(redisRateLimiter())
                    .setKeyResolver(userKeyResolver())))
            .uri("lb://CARDS"))

        .build();
  }

  /**
   * Replace the Resilience4j default timeout of 1s.
   * 
   * Quando se desliga o Loans e é feita a requisição para /sbdb/accounts/api/fetchCustomerDetails,
   * ele as vezes funciona já o retorno null de loans é mais rápido que 1 segundo do timeout padrão.
   * 
   * Todavia, ao desligar loans e cards e fazer a requisição para
   * /sbdb/accounts/api/fetchCustomerDetails, o retorno é mais demorado pois ambos estão off. Por
   * consequência o circuitbreaker do gateway é disparado (accountsCircuitBreaker), pois o timeout
   * padrão do gateway é 1s.
   * 
   * Sendo assim, é preciso aumentar o timeout padrão do gateway, para que assim ele aguarde e deixe
   * o fallback de loans e cards agir antes do circuitbreaker do gateway.
   */
  @Bean
  Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory
        .configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .timeLimiterConfig(
                TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build())
            .build());
  }

  /**
   * For each second, the user can only make one request.
   * 
   * - Rate Limiter pattern.
   */
  @Bean
  RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(1, 1, 1);
  }

  /**
   * The method below is used to set the keyresolver by user name. With this keyresolver, redis will
   * be able to count the number of requests per second. If the name is not present in the header,
   * the method will set the user as "anonymous".
   * 
   * The code below is based on redis docs. For production eviroments, this needs to be upgraded.
   * 
   * - Rate Limiter pattern.
   */
  @Bean
  KeyResolver userKeyResolver() {
    return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("user"))
        .defaultIfEmpty("anonymous");
  }

}
