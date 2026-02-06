# Mermaid DFD 繪圖範例

## 繪圖慣例

DFD 以 Mermaid `flowchart` 語法繪製，使用以下慣例：

| DFD 元素 | Mermaid 語法 | 形狀 |
|---------|-------------|------|
| 外部實體 | `entity["名稱"]` | 方括號（矩形） |
| Process | `process(("名稱"))` | 雙括號（圓形） |
| Data Store | `store[("名稱")]` | 圓柱體（資料庫形狀） |
| 同步資料流 | `-->｜標籤｜` | 實線箭頭 |
| 非同步資料流 | `-.->｜標籤｜` | 虛線箭頭 |
| Bounded Context | `subgraph` | 分組框 |

## Level 0 範例：電商系統

```mermaid
flowchart LR
    buyer["買家"]
    seller["賣家"]
    logistics["物流服務商"]
    payment["金流服務商"]

    buyer -->|"瀏覽商品、下單、付款"| system(("電商系統"))
    system -->|"訂單狀態、出貨通知"| buyer
    seller -->|"上架商品、管理訂單"| system
    system -->|"訂單通知、銷售報表"| seller
    system -->|"出貨請求"| logistics
    logistics -->|"物流狀態更新"| system
    system -->|"付款請求"| payment
    payment -->|"付款結果"| system
```

## Level 1 範例：電商系統 Bounded Context 分解

```mermaid
flowchart TB
    buyer["買家"]
    seller["賣家"]
    logistics["物流服務商"]
    payment["金流服務商"]

    subgraph 商品目錄Context
        catalog(("商品目錄管理"))
        catalogStore[("商品目錄儲存")]
        catalog --- catalogStore
    end

    subgraph 訂單Context
        order(("訂單管理"))
        orderStore[("訂單聚合儲存")]
        order --- orderStore
    end

    subgraph 付款Context
        pay(("付款處理"))
        payStore[("付款紀錄儲存")]
        pay --- payStore
    end

    subgraph 出貨Context
        shipping(("出貨管理"))
        shipStore[("出貨單儲存")]
        shipping --- shipStore
    end

    %% 外部實體互動
    buyer -->|"瀏覽商品"| catalog
    seller -->|"上架商品"| catalog
    buyer -->|"建立訂單"| order
    order -->|"付款請求"| pay
    pay -->|"付款請求（REST）"| payment
    payment -->|"付款結果"| pay

    %% 跨 Context 事件流
    order -.->|"訂單已建立事件（Kafka）"| shipping
    pay -.->|"付款已完成事件（Kafka）"| order
    shipping -->|"出貨請求（REST）"| logistics
    logistics -->|"物流狀態更新"| shipping
    shipping -.->|"已出貨事件（Kafka）"| order
```

## Level 2 範例：訂單 Context 內部展開

```mermaid
flowchart TB
    buyer["買家"]

    subgraph 訂單Context
        createOrder(("建立訂單"))
        cancelOrder(("取消訂單"))
        queryOrder(("查詢訂單"))

        orderWriteStore[("訂單聚合儲存\n（寫模型）")]
        orderReadStore[("訂單查詢儲存\n（讀模型）")]

        createOrder -->|"寫入"| orderWriteStore
        cancelOrder -->|"寫入"| orderWriteStore
        orderWriteStore -.->|"領域事件投影（非同步）"| orderReadStore
        orderReadStore -->|"讀取"| queryOrder
    end

    buyer -->|"下單資料"| createOrder
    buyer -->|"取消請求"| cancelOrder
    buyer -->|"訂單查詢"| queryOrder
    queryOrder -->|"訂單明細"| buyer

    createOrder -.->|"訂單已建立事件"| extEvent["其他 Context"]
    cancelOrder -.->|"訂單已取消事件"| extEvent
```

## 進階：Saga 流程範例

```mermaid
flowchart TB
    subgraph 訂單Saga
        saga(("訂單處理 Saga"))
        sagaStore[("Saga 狀態儲存")]
        saga --- sagaStore
    end

    subgraph 訂單Context
        order(("訂單管理"))
        orderStore[("訂單聚合儲存")]
        order --- orderStore
    end

    subgraph 庫存Context
        inventory(("庫存管理"))
        invStore[("庫存聚合儲存")]
        inventory --- invStore
    end

    subgraph 付款Context
        pay(("付款處理"))
        payStore[("付款紀錄儲存")]
        pay --- payStore
    end

    order -.->|"訂單已建立事件"| saga
    saga -->|"扣減庫存指令"| inventory
    inventory -.->|"庫存已扣減事件"| saga
    saga -->|"執行付款指令"| pay
    pay -.->|"付款已完成事件"| saga
    saga -.->|"訂單已確認事件"| order

    %% 補償流程
    inventory -.->|"庫存不足事件"| saga
    saga -.->|"訂單已拒絕事件"| order
```

## 樣式建議

- 使用 `TB`（上到下）或 `LR`（左到右）方向，依圖表複雜度選擇
- Level 0 適合 `LR`，Level 1+ 適合 `TB`
- `subgraph` 標題使用 Bounded Context 名稱
- 虛線箭頭（`-.->`)）一律用於非同步資料流
- 資料流標籤簡潔，格式為 `{內容}（{機制}）`
