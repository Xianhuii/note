package aop.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AspectA {
    @Pointcut("execution(* aop.component.*.*(..))")
    public void pointcut() {}

     @Before("pointcut()")
    public void before() {

     }
}
