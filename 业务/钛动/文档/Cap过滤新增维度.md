# 1 概要说明

  ﻿[​⁠​‍‌‬‬​‍⁠​​‍‌‬‬​⁠​​﻿​​﻿​‍​‌​​​⁠​⁠​⁠﻿‌​‌​‬‌​​⁠​‬W22 - 飞书云文档](https://biu6ihvvco.feishu.cn/wiki/XRaWwVCNEi0fUEkQ3lscscLHnye)

# 2 投放端
target_condition新增定向条件：
- attribute：dim_imp_cap_day
- operation：lt

# 3 竞价引擎
新增流量cap缓存，位于`/cache/bundle_cost`

  流量cap缓存加载逻辑：
```mysql
select CONCAT_WS('_', adv_id, package, country, affiliate_id, first_ssp, bundle_id, ad_format) dim,  
       SUM(imp_count) impCount  
from pac_dsp_imp_count_aggregate_cool_start  
where report_date = '2025-05-23'  # today
group by adv_id, package, country, affiliate_id, first_ssp, bundle_id, ad_format
```

竞价流程新增过滤：
1. 构建维度：adv_id+package_name+county+aff_id+first_ssp+bundleid+adformat
2. 获取缓存中该维度的impCount
3. 与定向条件比较进行过滤