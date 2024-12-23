# 初始化脚本
```sql
alter table af_container  
    add real_slot int default 1 null comment '实际使用的布隆过滤器槽个数',
    add config_slot int null comment '配置的布隆过滤器槽个数';
  
alter table af_sync  
    add progress bigint default 0 null comment '人群包已经同步的个数';
```