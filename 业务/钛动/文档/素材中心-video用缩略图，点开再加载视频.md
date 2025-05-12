- 分支：feature/video-capture
- 服务：adm
- 初始化脚本：

```JSON
alter table creative
    add thumbnail varchar(255) null comment '缩略图';
```

- 前端：creative新增thumbnail字段，如果存在，直接展示；如果不存在，需要读取视频