# 【shein】主adg、主ad更改监控
需求：
1. 新增main adg
2. 改动main adg
	1. Adg name
	2. Adg on/off
	3. Click url
	4. Deeplink
	5. 国家
	6. Os
	7. 预算
	8. 投放时段
	9. 出价
3. 新增main ad
涉及改动接口：
- com.tecdo.adm.delivery.controller.AdGroupController#saveParent
- com.tecdo.adm.delivery.controller.AdGroupController#updateListInfo
	- name
	- status
- com.tecdo.adm.delivery.controller.AdGroupController#updateParent
- com.tecdo.adm.delivery.controller.AdController#saveAdBatch