
```
create table ad_group_cost_info  
(  
    id                        int auto_increment comment '主键' primary key,  
    group_id                 int                                   not null comment 'adGroup id',  
    status                    int                                   null comment 'BaseStatusEnum',  
    cost_status                    int                                   null comment 'CalcStatusEnum',  
    create_time               datetime    default CURRENT_TIMESTAMP null comment '创建时间',  
    update_time               datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'  
) comment '广告组当日消耗信息表';  
  
create unique index idx_ad_group_id on ad_group_cost_info (group_id);
```
