package applicationcontext;

import bean.BeanObj;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;

public class AnnotationConfigApplicationContextScanDemo {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        ((AnnotationConfigRegistry) applicationContext).scan("bean");
        ((ConfigurableApplicationContext) applicationContext).refresh();
        BeanObj bean = applicationContext.getBean(BeanObj.class);
        System.out.println(bean.getValue());
    }
}
