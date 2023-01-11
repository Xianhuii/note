package ioc.beandefinitionreader;

import ioc.beandefinitionreader.component.ComponentA;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;

public class AnnotatedGenericBeanDefinitionMain {
    public static void main(String[] args) {
        AnnotatedBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(ComponentA.class);
        System.out.println(beanDefinition.getMetadata().getAnnotationTypes());
    }
}
