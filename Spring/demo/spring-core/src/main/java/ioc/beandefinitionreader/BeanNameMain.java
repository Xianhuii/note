package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentC;
import ioc.beandefinitionreader.component.ComponentD;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

import java.util.Arrays;

public class BeanNameMain {
    public static void main(String[] args) {
        // 创建registry
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        // 创建reader，指定registry
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        // 注册指定类作为bean，指定创建bean的方法
        reader.registerBean(ComponentD.class);
        // 打印ComponentC的BeanDefinition信息
        System.out.println(Arrays.toString(registry.getBeanDefinitionNames()));
    }
}
