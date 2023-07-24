package applicationcontext;

import bean.BeanObj;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;

public class AnnotationConfigApplicationContextRegisterDemo {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        ((AnnotationConfigRegistry) applicationContext).register(BeanObj.class);
        ((ConfigurableApplicationContext) applicationContext).refresh();
        BeanObj bean = applicationContext.getBean(BeanObj.class);
        System.out.println(bean.getValue());
    }
}
