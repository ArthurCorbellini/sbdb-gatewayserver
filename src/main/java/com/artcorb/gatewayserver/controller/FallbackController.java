package com.artcorb.gatewayserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class FallbackController {

  @RequestMapping("/contactSupport")
  public Mono<String> contactSupport() {
    // TODO send an e-mail to support team with the error, and change the return http status.
    return Mono.just("An error occured. Please try after some time or contact support team!");
  }

}
