package applicationcontext;

import applicationcontext.config.AppConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AnnotationConfigApplicationContextMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(AppConfig.class);
        context.scan("applicationcontext");
        context.refresh();
        AppConfig bean = context.getBean(AppConfig.class);
        System.out.println(bean);
    }
}
