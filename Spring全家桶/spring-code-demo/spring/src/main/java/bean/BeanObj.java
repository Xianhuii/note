
package bean;

import annotation.AspectAnnotation;
import org.springframework.stereotype.Component;

@Component
public class BeanObj {
    private final String value = "test";

    @AspectAnnotation
    public String getValue() {
        return value;
    }
}
