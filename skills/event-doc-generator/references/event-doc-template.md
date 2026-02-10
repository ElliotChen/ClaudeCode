# 事件文件模板

以下為每個 Event 獨立文件的模板。根據實際分析結果填入對應內容。

---

## 模板

```markdown
# {EventClassName}

> {一句話描述此事件的用途}

## 基本資訊

| 項目 | 內容 |
|------|------|
| 類別名稱 | `{完整類別名稱含 package}` |
| 事件方向 | {入站 (Inbound) / 出站 (Outbound) / 雙向 (Inbound / Outbound)} |
| 主題 Topic | {Topic 名稱} |
| 所屬 Bounded Context | {BoundedContext 名稱} |
| 關聯 Aggregate | {Aggregate 名稱，若適用} |
| 關聯 Saga | {Saga 名稱，若適用} |

## 欄位說明

| 欄位名稱 | 型別 | 說明 |
|----------|------|------|
| `{fieldName}` | `{Type}` | {欄位用途說明} |
| ... | ... | ... |

## 事件流向

### 發布端（Publisher）

- **位置**: `{發布此事件的類別與方法}`
- **觸發條件**: {描述何時會發布此事件}

### 消費端（Consumer）

- **位置**: `{處理此事件的類別與方法}`
- **處理邏輯**: {描述收到事件後的處理行為}

{若為 Saga 相關事件，加入以下區塊}

## Saga 關聯

| 項目 | 內容 |
|------|------|
| Saga 名稱 | `{SagaClassName}` |
| Association Property | `{associationProperty}` |
| 角色 | {Start Event / Intermediate Event / End Event} |

## 手動補入範例

> ⚠️ 以下範例用於系統異常時，由開發人員手動發送事件以修復資料狀態。
> 請務必確認欄位值正確後再執行。

### 方式一：透過 EventGateway 發送

```java
// 注入 EventGateway
@Autowired
private EventGateway eventGateway;

// 手動發送事件
{EventClassName} event = new {EventClassName}(
    {逐欄位填入範例值，附註解說明}
);
eventGateway.publish(event);
```

### 方式二：透過 GenericEventMessage 發送

```java
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventhandling.EventBus;

@Autowired
private EventBus eventBus;

{EventClassName} payload = new {EventClassName}(
    {逐欄位填入範例值，附註解說明}
);

eventBus.publish(GenericEventMessage.asEventMessage(payload));
```

### 方式三：透過 Axon Server REST API 發送（適用於 Axon Server 環境）

```bash
curl -X POST http://localhost:8024/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [{
      "payload": {
        "{fieldName}": "{sampleValue}",
        ...
      },
      "payloadType": "{完整類別名稱}",
      "payloadRevision": null
    }]
  }'
```

### 方式四：透過 kafka-console-producer 發送

```bash 

kafka-console-producer \
  --bootstrap-server <BROKER> \
  --topic {TopicName} <<'EOF'
{
  "type": "{EventTypeName}",
  "namespace": "{EventNameSpace}",
  "payload": {
    "{fieldName}": "{sampleValue}",
    ...
  }
}
EOF
```

---

## 填寫指引

- `事件方向`: 根據 Step 2 分析結果填入。若無法確定，標記 `⚠️ 待確認`。
- `欄位說明`: 保留原始英文欄位名稱，說明欄使用繁體中文。
- `手動補入範例`: 使用該事件的實際欄位，並提供合理的範例值（如 UUID、金額、時間戳等）。
- `Saga 關聯`: 僅在事件與 Saga 有關時才加入此區塊。
- 若事件使用 Lombok（如 `@Value` 或 `@Data`），仍需列出所有欄位。
- 若事件有繼承關係，需包含父類別的欄位。
