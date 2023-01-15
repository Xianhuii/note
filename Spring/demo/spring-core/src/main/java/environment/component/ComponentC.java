package environment.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ComponentC {
    @Value("${java.runtime.name}")
    private String runtimeName;

    public String getRuntimeName() {
        return runtimeName;
    }

    public void setRuntimeName(String runtimeName) {
        this.runtimeName = runtimeName;
    }
}
