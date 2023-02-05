package annotation.configuration.config;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;

@Configuration
@Order(1)
@ComponentScan
@ComponentScans(value = {@ComponentScan})
@Import(AppConfig.class)
@ImportResource
@PropertySource("")
@PropertySources(@PropertySource(""))
public class AppConfig {

    @Bean
    public Object object() {
        return new Object();
    }
}
