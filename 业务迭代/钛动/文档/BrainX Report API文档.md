# BrainX Report API文档
## 一、文档概述
### 1.1 文档目的
本文档旨在详细描述BrainX提供的对外接口，为外部开发者调用这些接口提供全面、准确的信息，包括接口的功能描述、请求参数、响应参数、调用示例等，以确保接口的正确使用和集成。
### 1.2 适用范围
本接口文档适用于所有需要调用BrainX对外接口的外部开发者和合作伙伴。
### 1.3 接口约定
- **请求方式**：支持 HTTP/HTTPS 协议，具体请求方式（如 GET、POST、PUT、DELETE 等）在每个接口中明确说明。
- **数据格式**：请求和响应数据均采用 JSON 格式。
- **字符编码**：统一使用 UTF - 8 字符编码。
## 二、接口列表
### 2.1 广告主报表数据查询接口
#### 2.1.1 接口概述
- **接口名称**：广告主报表数据查询
- **接口地址**：/adm/foreign/report
- **请求方式**：GET
- **功能描述**：查询广告主的报表数据。
#### 2.1.2 请求参数

| 参数名        | 参数位置 | 参数类型   | 是否必填 | 参数描述                                                                                                                                                                                   |
| ---------- | ---- | ------ | ---- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| token      | 路径参数 | string | 是    | 广告主token                                                                                                                                                                               |
| dimensions | 路径参数 | string | 否    | 查询维度，多个维度使用英文逗号分隔，默认按照最细维度查询。支持的维度如下：<br>campaign_id: campaign id<br>ad_group_id: ad group id<br>ad_id:  ad id<br>ad_format: ad format<br>ad_size: ad size<br>creative_id: creative id |
| startDate  | 路径参数 | string | 否    | 查询起始时间，格式：yyyy-MM-dd                                                                                                                                                                   |
| endDate    | 路径参数 | string | 否    | 查询结束时间，格式：yyyy-MM-dd                                                                                                                                                                   |

#### 2.1.3 响应参数

| 参数名     | 参数位置 | 参数类型          | 是否必填 | 参数描述    |
| ------- | ---- | ------------- | ---- | ------- |
| code    | 请求体  | integer       | 是    | code    |
| message | 请求体  | string        | 是    | message |
| data    | 请求体  | array[object] | 否    | 响应数据    |

data参数

| 参数名                   | 参数位置 | 参数类型      | 是否必填 | 参数描述 |
| ------------------------ | -------- | ------------- | -------- | -------- |
| campaignId               |          |               |          |          |
| campaignName             |          |               |          |          |
| adGroupId                |          |               |          |          |
| adGroupName              |          |               |          |          |
| adId                     |          |               |          |          |
| adName                   |          |               |          |          |
| adFormat                 |          |               |          |          |
| adSize                   |          |               |          |          |
| creativeId               |          |               |          |          |
| reportDate               |          |               |          |          |
| imp                      | 请求体   | number        | 是       | code     |
| click                    | 请求体   | string        | 是       | message  |
| ctr                      | 请求体   | array[object] | 否       | 响应数据 |
| cpm                      |          |               |          |          |
| cpc                      |          |               |          |          |
| spend                    |          |               |          |          |
| spendNotRt               |          |               |          |          |
| purchaseEventACount      |          |               |          |          |
| purchaseEventBCount      |          |               |          |          |
| purchaseEventBNotRtCount |          |               |          |          |
#### 2.1.4 响应示例
```json
{
  "code": 0,
  "message": "",
  "data": [
    {
      "imp": 0,
      "clicks": 0,
      "ctr": 0,
      "cpm": 0,
      "cpc": 0,
      "spend": 0,
      "spend_not_rt": 0,
      "purchase_event_a_count": 0,
      "purchase_event_a_not_rt_count": 0,
      "campaign_id": 0,
      "campaign_name": "",
      "ad_group_id": 0,
      "ad_group_name": "",
      "ad_id": 0,
      "ad_name": "",
      "ad_format": "",
      "ad_size": "",
      "creative_id": 0,
      "report_date": ""
    }
  ]
}
```
#### 2.1.5 错误码说明

| 错误码 | 错误信息                  | 错误描述        |
| --- | --------------------- | ----------- |
| 400 | failure               | 请求参数错误      |
| 401 | un authorized         | 未授权的 API 密钥 |
| 500 | internal server error | 服务器内部错误     |
## 三、认证与授权

### 3.1 认证方式
本系统采用API Key进行身份认证。
### 3.2 API Key 认证示例
在请求参数中添加 `token` 字段，值为分配给开发者的 API Key。例如：
```
http://example.com/api-example?token=your_token_here
```
## 四、异常处理
### 4.1 常见错误处理
当接口调用出现错误时，系统会返回相应的 HTTP 状态码和错误信息，开发者可以根据错误码和错误信息进行相应的处理。具体的错误码和错误信息在每个接口的文档中已经详细说明。
## 五、更新日志

| 版本号 | 更新日期           | 更新内容                 |
| --- | -------------- | -------------------- |
| 1.0 | 2025 - 02 - 11 | 初始版本发布，包含广告主报表数据查询接口 |