package codex.mmxxvi.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void createsCorsWebFilter() {
        CorsWebFilter filter = new CorsConfig().corsWebFilter();

        assertThat(filter).isNotNull();
    }
}
