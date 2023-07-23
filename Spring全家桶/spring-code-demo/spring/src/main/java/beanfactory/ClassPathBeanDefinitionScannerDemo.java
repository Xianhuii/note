package beanfactory;

import beanfactory.bean.BeanObj;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

public class ClassPathBeanDefinitionScannerDemo {
    public static void main(String[] args) {
        BeanFactory beanFactory = new DefaultListableBeanFactory();
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) beanFactory);
        scanner.scan("beanfactory.bean");
        BeanObj bean = beanFactory.getBean(BeanObj.class);
        System.out.println(bean.getValue());
    }
}
