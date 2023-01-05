# 联系方式
- 手机：18221205632
- Email：xianhuii@foxmail.com

# 个人信息
- 简先辉 / 男 / 1995
- 本科&硕士 / 同济大学-化学科学与工程学院
- 工作年限：1.5年
- 技术博客：https://www.cnblogs.com/Xianhuii
- 期望职位：Java工程师

# 工作经历

## 法信公证云（2021年11月~2023年1月）
根据需求进行设计和开发，参与项目包括电子数据保管平台、知产业务后台、公证处后台、仓储系统和百翎原创保护平台等，以下介绍几个比较典型的迭代。
### 1 公证处后台-电子公证书推送核验平台
核心功能是：将已出证订单的公证书和证据附件等信息推送到第三方核验平台进行上链，用户可以在核验平台进行核验和下载。
由于证据附件数量多（上百条）、文件大（超过5G），需要从OSS下载到本地进行解密，然后再上传到OBS，是个十分耗时的操作。因此在订单完成出证后，使用RabbitMQ进行异步处理，避免影响主业务流程。对于历史数据，使用定时任务进行推送。在证据附件解密过程中，考虑到大事务可能会造成数据库死锁，按单个证据维度进行下载解密，将大事务拆分成小事务。为了避免分布式环境下的重复推送，使用分布式锁按订单维度进行加锁。
目前正式环境已推送30W+条订单。
### 2 公证处后台-证据异步核验
业务需求

## 厦门立林科技有限公司（2021年7月~2021年11月）
### IoT云平台接入第三方


# 技术文章
深入学习官方文档和源码，总结在学习和工作过程中遇到的技术问题，如：
- [RequestMappingHandlerMapping请求地址映射的初始化流程](https://www.cnblogs.com/Xianhuii/p/16980975.html)
- [RequestMappingHandlerMapping的请求地址映射流程](https://www.cnblogs.com/Xianhuii/p/16988549.html)
- [RequestMappingHandlerAdapter详解](https://www.cnblogs.com/Xianhuii/p/17018699.html)
- [Spring MVC拦截器HandlerInterceptor全解！](https://www.cnblogs.com/Xianhuii/p/16990517.html)
- ……

# 技能清单
- 编程语言：Java，JavaScript
- 后端框架：Spring，Spring MVC，Spring Boot，Spring Cloud Alibaba，MyBatis，MyBatis Plus
- 前端框架：Vue2
- 数据库：MySQL，Redis
- 消息队列：RabbitMQ
- 工具：Git，SVN
- 其他：熟悉Spring Web MVC源码，MySQL优化

# 致谢
感谢您花时间阅读我的简历，期待能有机会和您共事。