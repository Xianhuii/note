package com.example.component;

import org.springframework.aop.scope.ScopedObject;
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
