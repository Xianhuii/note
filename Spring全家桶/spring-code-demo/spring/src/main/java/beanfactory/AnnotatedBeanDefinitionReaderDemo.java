package beanfactory;

import beanfactory.bean.BeanObj;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

public class AnnotatedBeanDefinitionReaderDemo {
    public static void main(String[] args) {
        BeanFactory beanFactory = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader((BeanDefinitionRegistry) beanFactory);
        reader.register(BeanObj.class);
        BeanObj bean = beanFactory.getBean(BeanObj.class);
        System.out.println(bean.getValue());
    }
}
