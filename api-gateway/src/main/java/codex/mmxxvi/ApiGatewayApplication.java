package codex.mmxxvi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.openfeign.EnableFeignClients;

import codex.mmxxvi.config.CustomLoadBalancerConfiguration;

@SpringBootApplication
@EnableFeignClients
@LoadBalancerClients(defaultConfiguration = CustomLoadBalancerConfiguration.class)
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
