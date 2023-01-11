package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentA;
import ioc.beandefinitionreader.config.AppConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

import java.util.Arrays;

public class AnnotatedBeanDefinitionReaderMain {
    public static void main(String[] args) {
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        reader.registerBean(AppConfig.class);
        System.out.println(registry.getBeanDefinitionCount());
        System.out.println(Arrays.toString(registry.getBeanDefinitionNames()));
    }
}
