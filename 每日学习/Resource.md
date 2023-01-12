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
`ResourceLoader`是加载


![[ResourceLoader.png]]

# 3 ResourcePatternResolver

