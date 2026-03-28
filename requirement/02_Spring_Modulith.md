# Add Spring Modulith features
使用Spring Modulith 2.0.3達成下列需求
- 先理解Spring Modulith的核心概念與實作重點
- 模組包含部門(Department)，員工(Employee)
- 員工有職級(Rank)與狀態(Status)
- 部門對於員工可以雇用，解雇，調職，升職，降級；這些操作會影響職級與狀態
- event的傳遞使用jdbc
- 提供對外可用的api，並支援openapi，產生curl的使用範例
- 包含完成的Test，但需區分Integration Test與Unit Test
- DB使用Postgresql 18，docker image 使用postgres:18-alpine，變更管理使用flyway，
- 並使用testcontainer 2.0.3 做為測試工具之一，include testcontainer-bom 做為dependency-management ，並include testcontainers-postgresql， testcontainers-junit-jupiter
- 若有使用UUID，需使用UUID V7
