package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentA;
import ioc.beandefinitionreader.config.AppConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

public class AnnotatedBeanDefinitionReaderMain {
    public static void main(String[] args) {
        // 创建registry
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        // 创建reader，指定registry
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        // 注册指定类作为bean
        reader.registerBean(ComponentA.class);
        // 打印ComponentA的BeanDefinition信息
        System.out.println(registry.getBeanDefinition("componentA"));
    }
}
