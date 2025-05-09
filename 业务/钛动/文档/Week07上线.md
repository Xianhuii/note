# 【shein】主adg、主ad更改监控（feature/adv_modify_alarm）

配置：
- alarm-ding-talk.robots.adv-modify.secret：SECe6bdb76c617b01580f47dfb79d19b8c8ceced253f5d031cd415eb5042ee0b968
- alarm-ding-talk.robots.adv-modify.access-token：d6e8f2aacf95a6677883b690954a89fecf3d2f8c2486f65fa86820e2b4f73f00
- alarm-feishu.webhook.adv-modify：https://open.feishu.cn/open-apis/bot/v2/hook/21627209-23d5-4aee-9f34-82f7310a0cd1

# 【shein】提供广告信息接口 &【shein】提供报表接口（feature/foreign_api）
nginx路由配置：
- /adm/adv：转发到后端
```
location /adm/adv/ {
	proxy_pass http://brainx-adm:8080/adm/adv/;
}
```
token配置：字典adv_foreign_token

# 【shein】主adg的出价联动sub adg出价调整（feature/adv_modify_alarm）
无

# 流量冷启动V1版——冷启池定时任务（feature/cold-start-v1-adm）（feature/cold-start-v1-pool）
- 投放端数据库初始化
```sql
alter table ad_group  
    add exploration_mode tinyint default 0 not null comment '探索策略：0：关闭；1：冷启动探索策略';  
  
alter table ad_group  
    add cold_boot_budget_percent double null comment '冷启动预算比例';
```
- 定时任务数据库初始化
```sql
create table if not exists cool_start_pool  
(  
    id                int auto_increment comment '主键' primary key,  
    adg_id            int          null comment '广告组id',  
    adv_id            int          null comment '广告主id',  
    package_name      varchar(255) null comment '广告包名',  
    aff_id            int          null comment '渠道id',  
    bundle            varchar(255) null comment '包名',  
    first_ssp         varchar(255)     null comment '包名',  
    country           varchar(16)  null comment '国家三位码',  
    format            varchar(36)  null comment '广告类型',  
    mode              tinyint      not null default 0 comment '0表示曝光未达到目标值，处于冷启阶段;1表示曝光达到目标值，渡过冷启阶段;后续存在扩展更多枚举值的可能',  
    create_time       datetime     null comment '当前流量初始化到该adg的冷启池的时间',  
    first_finish_time datetime     null comment '第一次流量达到曝光条件的时间',  
    last_finish_time  datetime     null comment '最近一次流量达到曝光条件的时间',  
    status            tinyint      not null default 0 comment '1表示有效，0表示无效'  
) comment '冷启池表' collate = utf8mb4_unicode_ci;
```
- 配置：
	- ding-talk.robots.cool-start-alarm.secret: SEC9b3493af2d2404b73dae7f0cfadb87f5b33ec8a5e5330340020affe021104baa
	- ding-talk.robots.cool-start-alarm.access-token: f1a13a89f5f981307d555034d94c8afed85e78ba3213c6ae2e32f790e389c782
- 创建定时任务
# Week06 bug