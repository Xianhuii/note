package aspect;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AspectBean {
    @Before("@annotation(annotation.AspectAnnotation)")
    public void before() {
        System.out.println("before");
    }

    @After("@annotation(annotation.AspectAnnotation)")
    public void after() {
        System.out.println("after");
    }
}
