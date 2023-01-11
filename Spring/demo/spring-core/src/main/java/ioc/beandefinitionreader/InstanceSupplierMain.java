package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentA;
import ioc.beandefinitionreader.component.ComponentB;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

public class InstanceSupplierMain {
    public static void main(String[] args) {
        // 创建registry
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        // 创建reader，指定registry
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        // 注册指定类作为bean，指定创建bean的方法
        reader.registerBean(ComponentB.class, () -> {
            ComponentB componentB = new ComponentB();
            componentB.setName("instanceSupplier");
            return componentB;
        });
        // 打印ComponentB的信息
        ComponentB componentB = ((BeanFactory) registry).getBean(ComponentB.class);
        System.out.println(componentB.getName());
    }
}
