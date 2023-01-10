package ioc.container;

import ioc.container.config.AppConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        AppConfig appConfig = context.getBean(AppConfig.class);
        System.out.println(appConfig);
    }
}
