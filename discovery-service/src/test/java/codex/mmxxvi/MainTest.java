package codex.mmxxvi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void applicationClassEnablesEurekaServer() {
        assertThat(Main.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(Main.class.isAnnotationPresent(EnableEurekaServer.class)).isTrue();
    }
}
