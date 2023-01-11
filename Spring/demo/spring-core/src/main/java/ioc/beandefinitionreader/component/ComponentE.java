package ioc.beandefinitionreader.component;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ComponentE {
}
