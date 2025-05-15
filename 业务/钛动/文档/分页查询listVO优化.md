### 1 AdGroup
请求：
```bash
curl --location --request POST 'http://localhost:8080/adm/ad-group/pageV3' \

--header 'token: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9.eyJpc3MiOiJpc3N1c2VyIiwiYXVkIjoiYXVkaWVuY2UiLCJyb2xlX25hbWUiOiJyb290Iiwicm9sZV9pZCI6IjEiLCJhZHZfaWQiOiIiLCJyZWFsX25hbWUiOiLnroDlhYjovokiLCJ0b2tlbl90eXBlIjoiYWNjZXNzX3Rva2VuIiwiYWNjb3VudCI6Im1heC5qaWFuIiwibmJmIjoxNzQ3MTg1OTA4fQ.mOccMDwGwZSKRBx4skeAB9EJzHk4fn2_dnlfd1GYZVJabU3otEoqLT5zkV7lcDEl' \

--header 'Content-Type: application/json' \

--data-raw '{

    "size": 100,

    "current": 1,

    "startDate": "2025-05-13",

    "endDate": "2025-05-14",

    "adGroupSearchP": true,

    "campaignIds": "",

    "advIds": "",

    "descs": "id",

    "ascs": null,

    "campaignStatus": 1,

    "adGroupStatus": 1

}'
```
优化前：60.80s
优化后：9.44s