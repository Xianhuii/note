package annotation.configuration;

import annotation.configuration.config.AppConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
//        beanFactory();
        applicationContext();
    }

    public static void beanFactory() {
        BeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(beanDefinitionRegistry);
        reader.registerBean(AppConfig.class);
        Object object = beanFactory.getBean("object");
        System.out.println(object);
    }

    public static void applicationContext() {
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Object object = context.getBean("object");
        System.out.println(object);
    }
}
