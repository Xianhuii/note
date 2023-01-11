package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentC;
import ioc.beandefinitionreader.component.ComponentE;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;

public class ScopeProxyMain {
    public static void main(String[] args) {
        // 创建registry
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        // 创建reader，指定registry
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        // 注册指定类作为bean，指定创建bean的方法
        reader.registerBean(ComponentE.class);
        // 打印ComponentE的信息
        System.out.println(((BeanFactory) registry).getBean("componentE"));
        System.out.println(((BeanFactory) registry).getBean("scopedTarget.componentE"));
        System.out.println(((BeanFactory) registry).getBean(ComponentE.class));
        System.out.println(((BeanFactory) registry).getBean(ComponentE.class).getClass());
//        System.out.println((ScopedObject)(((BeanFactory) registry).getBean(ComponentE.class)).getTargetObject());
    }
}
