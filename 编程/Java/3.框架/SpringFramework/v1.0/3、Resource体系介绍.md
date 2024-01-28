# 1 Resource
![[Resource 1.png]]

`org.springframework.core.io.Resource`是Spring对底层资源的统一抽象。它提供了访问资源的统一方法：
- 获取资源信息。
- 获取输入流。
- 获取对应`File`或`URI`对象。

`Resource`在Spring IoC容器中有着广泛应用，主要作用是读取不同形式的依赖配置。例如，`ClassPathBeanDefinitionScanner`会扫描指定路径的所有`bean`对象，将路径封装成`UrlResource`或`FileUrlResource`。

Spring提供了很多内置实现类，这里简单介绍一些典型的。

## 1.1 UrlResource
![[UrlResource.png]]
`UrlResource`封装了`java.net.URL`对象，提供了对`url`资源的统一处理。

通过指定标准的`url`地址前缀，就可以很简单地创建`UrlResource`对象：
- `file:`：文件系统路径。
- `https:`：通过HTTPS协议获取资源。
- `ftp:`：通过FTP协议获取资源。
- ……

## 1.2 ClassPathResource
![[ClassPathResource.png]]
`ClassPathResource`提供了对类路径下资源的统一处理。

如果是在文件系统类路径下的资源（非JAR包中），会解析成`java.io.File`对象，否则会解析成`java.net.URL`对象。

## 1.3 FileSystemResource
![[FileSystemResource.png]]
`FileSystemResource`提供了对文件系统中资源的统一处理。

## 1.4 PathResource
![[PathResource.png]]
`PathResource`提供了对文件系统中资源的统一处理，它会将资源解析成`java.nio.file.Path`对象进行处理。

## 1.5 ServletContextResource
![[ServletContextResource.png]]
`ServletContextResource`提供了对Web应用根路径下资源的统一处理。

## 1.6 ByteArrayResource
![[ByteArrayResource.png]]
`ByteArrayResource`缓存了`byteArray`数据，可以重复获取。

## 1.7 InputStreamResource
![[InputStreamResource.png]]
`InputStreamResource`是对`InputStream`的封装，它应该作为`Resource`的兜底选择。

# 2 ResourceLoader
![[ResourceLoader 1.png]]
`ResourceLoader`是加载类路径或文件系统资源的工具类，它可以根据指定的路径获取对应的`Resource`。

![[ResourceLoader 2.png]]
`ResourceLoader`的基础实现类是`DefaultResourceLoader`，其中定义了加载资源的基本逻辑，默认会解析成`ClassPathResource`或`ClassPathContextResource`对象。

`ServletContextResourceLoader`、`FileSystemResourceLoader`和`ClassRelativeResourceLoader`子类会重写`getResourceByPath()`方法，分别解析成对应的`XxxResource`对象。

需要注意的是，`AbstractApplicationContext`也继承了`DefaultResourceLoader`类，它会采取相同的逻辑加载资源。

但是，`AbstractApplicationContext`的子类`GenericApplicationContext`重写了这一规则，它内部持有`ResourceLoader`对象，可以动态进行资源解析，只有在未设置的情况下才会使用默认规则。

## 2.1 DefaultResourceLoader
`DefaultResourceLoader`是`ResourceLoader`的默认实现类，它的`getResource()`会依次使用以下方式进行解析：
1. 使用`protocolResolvers`成员变量，根据协议进行解析。
2. 如果以`/`开头，调用`getResourceByPath()`方法解析，默认返回`ClassPathContextResource`对象。
3. 如果以`classpath:`开头，返回`ClassPathResource`对象。
4. 如果是文件协议，返回`FileUrlResource`对象。
5. 如果不是文件协议，返回`UrlResource`对象
6. 如果以上都不能解析，再次调用`getResourceByPath()`方法解析，默认返回`ClassPathContextResource`对象。

`DefaultResourceLoader#getResource()`方法源码如下：
```java
public Resource getResource(String location) {  
   Assert.notNull(location, "Location must not be null");  
   // 使用`protocolResolvers`成员变量，根据协议进行解析
   for (ProtocolResolver protocolResolver : getProtocolResolvers()) {  
      Resource resource = protocolResolver.resolve(location, this);  
      if (resource != null) {  
         return resource;  
      }  
   }  
   // 如果以`/`开头，调用getResourceByPath()方法解析，默认返回ClassPathContextResource对象。
   if (location.startsWith("/")) {  
      return getResourceByPath(location);  
   }  
   else if (location.startsWith(CLASSPATH_URL_PREFIX)) {  
      return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());  
   }  
   else {  
      try {  
         // Try to parse the location as a URL...  
         URL url = new URL(location);  
         return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));  
      }  
      catch (MalformedURLException ex) {  
         // No URL -> resolve as resource path.  
         return getResourceByPath(location);  
      }  
   }  
}
```

`DefaultResourceLoader#getResourceByPath()`方法默认返回`ClassPathContextResource`对象：
```java
protected Resource getResourceByPath(String path) {  
   return new ClassPathContextResource(path, getClassLoader());  
}
```

## 2.2 ServletContextResourceLoader
`ServletContextResourceLoader#getResourceByPath()`方法会返回`ServletContextResource`对象：
```java
protected Resource getResourceByPath(String path) {  
   return new ServletContextResource(this.servletContext, path);  
}
```

## 2.3 FileSystemResourceLoader
`FileSystemResourceLoader#getResourceByPath()`方法会返回`FileSystemContextResource`对象：
```java
protected Resource getResourceByPath(String path) {  
   if (path.startsWith("/")) {  
      path = path.substring(1);  
   }  
   return new FileSystemContextResource(path);  
}
```

## 2.4 ClassRelativeResourceLoader
`ClassRelativeResourceLoader#getResourceByPath()`方法会返回`ClassRelativeContextResource`对象：
```java
protected Resource getResourceByPath(String path) {  
   return new ClassRelativeContextResource(path, this.clazz);  
}
```

## 2.5 AbstractApplicationContext
`AbstractApplicationContext`继承了`DefaultResourceLoader`，但是它没有重写`getResourceByPath()`方法，因此还是会返回`ClassPathContextResource`对象。

但是，`AbstractApplicationContext`的子类`GenericApplicationContext`重写了`getResourceByPath()`方法，它内部持有`ResourceLoader`对象，可以动态进行资源解析，只有在未设置的情况下才会使用默认规则：
```java
public Resource getResource(String location) {  
   if (this.resourceLoader != null) {  
      for (ProtocolResolver protocolResolver : getProtocolResolvers()) {  
         Resource resource = protocolResolver.resolve(location, this);  
         if (resource != null) {  
            return resource;  
         }  
      }  
      return this.resourceLoader.getResource(location);  
   }  
   return super.getResource(location);  
}
```

# 3 ResourcePatternResolver
![[ResourcePatternResolver.png]]
`ResourcePatternResolver`继承了`ResourceLoader`接口，并新增了`getResources()`方法，可以根据通配符等路径模式获取资源。

![[ResourcePatternResolver 1.png]]
`ResourcePatternResolver`的基础实现类是`PathMatchingResourcePatternResolver`，其中定义了加载资源的基本逻辑。

需要注意的是，`Application`继承了`ResourcePatternResolver`接口，因此它本身就具备加载资源的功能。

## 3.1 PathMatchingResourcePatternResolver
`PathMatchingResourcePatternResolver#getResources()`方法定义了加载资源的基本逻辑：
```java
public Resource[] getResources(String locationPattern) throws IOException {  
   Assert.notNull(locationPattern, "Location pattern must not be null");  
   // 以"classpath*:"开头
   if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {  
      // 如果路径满足pathMatcher规则，按规则加载资源
      if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {  
         return findPathMatchingResources(locationPattern);  
      }  
      // 如果路径不满足pathMatcher规则，查找所有类路径资源
      else {  
         return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));  
      }  
   }  
   // 不是以"classpath*:"开头
   else {  
      // Generally only look for a pattern after a prefix here,  
      // and on Tomcat only after the "*/" separator for its "war:" protocol.      
      int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :  
            locationPattern.indexOf(':') + 1);  
      if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {  
         // a file pattern  
         return findPathMatchingResources(locationPattern);  
      }  
      else {  
         // a single resource with the given name  
         return new Resource[] {getResourceLoader().getResource(locationPattern)};  
      }  
   }  
}
```

`PathMatchingResourcePatternResolver#findPathMatchingResources()`方法会查找符合规则的资源（JAR文件、ZIP文件或文件系统中）：
```java
protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {  
   // 解析根路径：例如通配符前的地址（“/WEB-INF/*.xml” → “/WEB-INF/”）
   String rootDirPath = determineRootDir(locationPattern);  
   // 解析子路径：“/WEB-INF/*.xml” → “*.xml”
   String subPattern = locationPattern.substring(rootDirPath.length());  
   // 递归获取根路径的资源：默认调用DefaultResourceLoader获取资源
   Resource[] rootDirResources = getResources(rootDirPath);  
   Set<Resource> result = new LinkedHashSet<>(16);  
   // 遍历根路径，继续查找符合规则的资源
   for (Resource rootDirResource : rootDirResources) {  
      rootDirResource = resolveRootDirResource(rootDirResource);  
      URL rootDirUrl = rootDirResource.getURL();  
      if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {  
         URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);  
         if (resolvedUrl != null) {  
            rootDirUrl = resolvedUrl;  
         }  
         rootDirResource = new UrlResource(rootDirUrl);  
      }  
      if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {  
         result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));  
      }  
      else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {  
         result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));  
      }  
      else {  
         result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));  
      }  
   }  
   if (logger.isTraceEnabled()) {  
      logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);  
   }  
   return result.toArray(new Resource[0]);  
}
```

`PathMatchingResourcePatternResolver#findAllClassPathResources()`方法会查找类路径上的资源：
```java
protected Resource[] findAllClassPathResources(String location) throws IOException {  
   String path = location;  
   if (path.startsWith("/")) {  
      path = path.substring(1);  
   }  
   Set<Resource> result = doFindAllClassPathResources(path);  
   if (logger.isTraceEnabled()) {  
      logger.trace("Resolved classpath location [" + location + "] to resources " + result);  
   }  
   return result.toArray(new Resource[0]);  
}
```

## 3.2 AbstractApplicationContext
`AbstractApplicationContext`有个`resourcePatternResolver`成员变量，它的`getResources()`方法会交给这个成员变量执行：
```java
public Resource[] getResources(String locationPattern) throws IOException {  
   return this.resourcePatternResolver.getResources(locationPattern);  
}
```

`GenericApplicationContext`会重写这个方法：
```java
public Resource[] getResources(String locationPattern) throws IOException {  
   if (this.resourceLoader instanceof ResourcePatternResolver) {  
      return ((ResourcePatternResolver) this.resourceLoader).getResources(locationPattern);  
   }  
   return super.getResources(locationPattern);  
}
```

# 4 ResourceLoader和ApplicationContext的关系
![[ResourcePatternResolver 2.png]]
`ApplicationContext`继承了`ResourcePatternResolver`接口，实现了`getResource()`和`getResources()`方法。

# 5 ResourceLoaderAware
![[ResourceLoaderAware.png]]
只要某个`bean`实现了`ResourceLoaderAware`接口，在`ApplicationContext`实例化`bean`对象时，会将自身作为形参，触发`setResourceLoader()`方法。

具体源码位于`ApplicationContextAwareProcessor#invokeAwareInterfaces()`方法：
```java
private void invokeAwareInterfaces(Object bean) {  
   if (bean instanceof EnvironmentAware) {  
      ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());  
   }  
   if (bean instanceof EmbeddedValueResolverAware) {  
      ((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);  
   }  
   if (bean instanceof ResourceLoaderAware) {  
      ((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);  
   }  
   if (bean instanceof ApplicationEventPublisherAware) {  
      ((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);  
   }  
   if (bean instanceof MessageSourceAware) {  
      ((MessageSourceAware) bean).setMessageSource(this.applicationContext);  
   }  
   if (bean instanceof ApplicationStartupAware) {  
      ((ApplicationStartupAware) bean).setApplicationStartup(this.applicationContext.getApplicationStartup());  
   }  
   if (bean instanceof ApplicationContextAware) {  
      ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);  
   }  
}
```
