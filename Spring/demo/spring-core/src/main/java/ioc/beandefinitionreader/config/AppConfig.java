package ioc.beandefinitionreader.config;

import ioc.beandefinitionreader.component.ComponentA;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ComponentA componentA() {
        return new ComponentA();
    }
}
