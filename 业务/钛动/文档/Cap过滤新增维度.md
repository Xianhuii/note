# 1 概要说明

  ﻿[​⁠​‍‌‬‬​‍⁠​​‍‌‬‬​⁠​​﻿​​﻿​‍​‌​​​⁠​⁠​⁠﻿‌​‌​‬‌​​⁠​‬W22 - 飞书云文档](https://biu6ihvvco.feishu.cn/wiki/XRaWwVCNEi0fUEkQ3lscscLHnye)

# 2 表结构及ER图
target_condition新增定向条件：
- attribute：dim_imp_cap_day
- operation：lt

# 3 流程图

  cap缓存：
```mysql
select CONCAT_WS('_', adv_id, package, country, affiliate_id, first_ssp, bundle_id, ad_format) dim,  
       SUM(imp_count) impCount  
from pac_dsp_imp_count_aggregate_cool_start  
where report_date = '2025-03-21'  
group by adv_id, package, country, affiliate_id, first_ssp, bundle_id, ad_format
```

# 4 接口文档


# 5 修改点

# 6 CheckList