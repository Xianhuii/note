package aop;

import aspect.AspectBean;
import bean.BeanObj;
import config.AopConfiguration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

public class AnnotationConfigApplicationContextAopDemo {
    public static void main(String[] args) {
        BeanFactory beanFactory = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader((BeanDefinitionRegistry) beanFactory);
        reader.register(AopConfiguration.class, AspectBean.class, BeanObj.class);
        BeanObj bean = beanFactory.getBean(BeanObj.class);
        System.out.println(bean.getValue());
    }
}
