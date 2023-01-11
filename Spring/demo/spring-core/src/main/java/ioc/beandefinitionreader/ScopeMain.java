package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentB;
import ioc.beandefinitionreader.component.ComponentC;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

public class ScopeMain {
    public static void main(String[] args) {
        // 创建registry
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        // 创建reader，指定registry
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        // 注册指定类作为bean，指定创建bean的方法
        reader.registerBean(ComponentC.class);
        // 打印ComponentC的BeanDefinition信息
        System.out.println(registry.getBeanDefinition("componentC").getScope());
    }
}
