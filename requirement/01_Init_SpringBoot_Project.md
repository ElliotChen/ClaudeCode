# 實作總則
若遇到不確的的規範可以透過網路查詢瞭解，
例如Log4j，Spring Modulith

# 建立 Basic Spring Boot Project
建立一個基於Spring Boot 版本 4.0.3的專案在此空白目錄下，包含完整的設定檔與SpringBoot Application主程式，並依下列要求進行設定與改變
- group名稱為tw.elliot, artifact id為cctest
- 使用Maven做為專案管理工具
- 使用JDK 25
- 使用lombok做為builder, log工具，而且不使用logback而改用log4j2做為logging模組，因避免多次execlude，需include dependency org.springframework.boot:spring-boot-starter，並excelude spring-boot-starter-logging
- 使用application.yaml而非application.properties做為設定檔案
- 加入actuator功能，開啟所有的endpoint。
- include dependency org.springframework.boot:spring-boot-starter-opentelemetry加入opentelemetry tracing功能並關閉metrics export;必需配合使用spring boot的`management.tracing.*` 和 `management.otlp.*`設定，而非`otel.*`
- 在log中顯示trace id與span id，log pattern格式需符合log4j2的規範。
- 加入Controller，package為tw.elliot.cctest.ctrl，endpoint路徑為"/ctrl/hello"，回傳內容為"Hello, World!"，加入合適的log。