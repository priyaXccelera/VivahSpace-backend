package com.example.proposalservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Bean(name = "loadBalancedRestTemplate")
  @LoadBalanced
  public RestTemplate loadBalancedRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new AuthHeaderForwardingInterceptor());
    return restTemplate;
  }
}
