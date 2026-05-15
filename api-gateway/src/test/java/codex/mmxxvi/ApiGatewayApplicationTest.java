package codex.mmxxvi;

import codex.mmxxvi.config.CustomLoadBalancerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayApplicationTest {

    @Test
    void applicationClassHasGatewayBootstrapAnnotations() {
        assertThat(ApiGatewayApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(ApiGatewayApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();

        LoadBalancerClients loadBalancerClients = ApiGatewayApplication.class.getAnnotation(LoadBalancerClients.class);
        assertThat(loadBalancerClients).isNotNull();
        assertThat(loadBalancerClients.defaultConfiguration()).contains(CustomLoadBalancerConfiguration.class);
    }
}
