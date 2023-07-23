
package beanfactory.bean;

import org.springframework.stereotype.Component;

@Component
public class BeanObj {
    private final String value = "test";

    public String getValue() {
        return value;
    }
}
