# 1 为什么使用缓存？
> 高性能、高并发。

缓存主要是用来提高获取数据的速度，通过将一些热点数据存储在缓存中，可以大大提高业务处理的速度，因此可以提高系统的性能和并发能力。

在实际业务场景中，也可以用来缓存一些特殊数据，例如登录用户的`token`、分布式锁等。

# 2 Redis有哪些数据类型？
> string，list，hash，set，zset，

string可以存储字符串：
```bash
set name Xianhuii
```

list可以存储有序列表：
```bash
rpush app QQ WeChat Dingding
```

hash可以存储多个键值对：
```bash
hset person name Xianhuii
hset person age 18
```

set可以存储

# 3 Redis的过期策略？

# 4 Redis如何持久化？
