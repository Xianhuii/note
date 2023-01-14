package ioc.environment;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

public class EnvironmentMain {
    public static void main(String[] args) {
        Environment environment = new StandardEnvironment();
        System.out.println(environment);
    }
}
