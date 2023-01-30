package aop;

import aop.component.ComponentA;
import aop.config.AopConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AopMain {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AopConfig.class);
//        AspectA aspectA = context.getBean(AspectA.class);
//        System.out.println(aspectA);
        ComponentA componentA = context.getBean(ComponentA.class);
//        System.out.println(componentA);
        componentA.test();
//        BeanDefinitionRegistry beanDefinitionRegistry = new DefaultListableBeanFactory();
//        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(beanDefinitionRegistry);
//        reader.registerBean(AopConfig.class);
//        BeanFactory beanFactory = (BeanFactory) beanDefinitionRegistry;
//        ComponentA componentA2 = beanFactory.getBean(ComponentA.class);
//        System.out.println(componentA2);
//        componentA2.test();
    }
}
