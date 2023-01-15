package environment;

import environment.component.ComponentC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class EnvironmentMain {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext("environment");
        ComponentC bean = context.getBean(ComponentC.class);
        System.out.println(bean.getRuntimeName());
    }
}
