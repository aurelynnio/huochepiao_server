package codex.mmxxvi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServerTest {

    @Test
    void applicationClassEnablesSpringCloudConfigServer() {
        assertThat(ConfigServer.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(ConfigServer.class.isAnnotationPresent(EnableConfigServer.class)).isTrue();
    }
}
