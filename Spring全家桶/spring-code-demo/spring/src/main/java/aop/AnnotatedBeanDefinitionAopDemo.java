package aop;

import aspect.AspectBean;
import bean.BeanObj;
import config.AopConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AnnotatedBeanDefinitionAopDemo {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AopConfiguration.class, AspectBean.class, BeanObj.class);
        BeanObj bean = applicationContext.getBean(BeanObj.class);
        System.out.println(bean.getValue());
    }
}
