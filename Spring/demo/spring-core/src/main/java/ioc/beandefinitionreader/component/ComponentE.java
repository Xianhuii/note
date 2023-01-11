package ioc.beandefinitionreader.component;

import org.springframework.aop.scope.DefaultScopedObject;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ComponentE implements ScopedObject{

    @Override
    public Object getTargetObject() {
        return this;
    }

    @Override
    public void removeFromScope() {

    }
}
