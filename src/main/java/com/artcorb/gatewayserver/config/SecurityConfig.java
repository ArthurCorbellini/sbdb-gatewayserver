package com.artcorb.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity serverHttpSecurity) {
    serverHttpSecurity
        .authorizeExchange(ex -> ex.pathMatchers(HttpMethod.GET).permitAll()
            .pathMatchers("/sbdb/accounts/**").authenticated().pathMatchers("/sbdb/cards/**")
            .authenticated().pathMatchers("/sbdb/loans/**").authenticated())
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

    serverHttpSecurity.csrf(csrfSpec -> csrfSpec.disable());
    return serverHttpSecurity.build();
  }

}
