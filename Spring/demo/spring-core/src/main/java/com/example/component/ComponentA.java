package com.example.component;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Profile("default")
@Lazy
@Component
@Configuration
@Controller
public class ComponentA {
}
