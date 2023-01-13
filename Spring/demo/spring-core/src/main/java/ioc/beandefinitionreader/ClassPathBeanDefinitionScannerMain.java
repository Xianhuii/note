package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentE;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

import java.util.Arrays;

public class ClassPathBeanDefinitionScannerMain {
    public static void main(String[] args) {
        // 创建registry
        BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
        // 创建scanner，指定registry
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        // 扫描指定包下的bean
        scanner.scan("ioc.beandefinitionreader.component");
        //
        System.out.println(registry.getBeanDefinitionCount());
        System.out.println(Arrays.toString(registry.getBeanDefinitionNames()));
    }
}
