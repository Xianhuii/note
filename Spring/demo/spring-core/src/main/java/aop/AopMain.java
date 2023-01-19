package aop;

import aop.component.ComponentA;
import aop.config.AopConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AopMain {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AopConfig.class);
//        AspectA aspectA = context.getBean(AspectA.class);
//        System.out.println(aspectA);
        ComponentA componentA = context.getBean(ComponentA.class);
        System.out.println(componentA);
        componentA.test();
    }
}
