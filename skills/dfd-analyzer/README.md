# DFD 文件產生技能（Spring Boot + DDD）

## 簡介

此技能用於為採用 **Spring Boot + DDD 架構**的專案產生結構化的資料流程圖（DFD）文件。所有產出採用繁體中文，圖表使用 Mermaid 語法，可直接嵌入 Markdown 架構文件。

## 檔案總覽

| 檔案 | 說明 |
|------|------|
| [SKILL.md](SKILL.md) | 技能主文件 — 核心原則、產出流程與文件模板 |
| [references/dfd-levels.md](references/dfd-levels.md) | DFD 層次分解指南 — Level 0~3 的分解原則與常見錯誤 |
| [references/ddd-mapping.md](references/ddd-mapping.md) | DDD 對應規則 — DFD 元素與 DDD 概念的對應、命名規範、Spring Boot 技術元件處理方式 |
| [references/mermaid-examples.md](references/mermaid-examples.md) | Mermaid 繪圖範例 — 各層次的完整範例圖表，含 CQRS 與 Saga 進階場景 |

## 核心設計理念

1. **以領域邊界驅動分解** — DFD 的 Process 對齊 Bounded Context，而非技術模組
2. **使用通用語言命名** — 所有標籤遵循 Ubiquitous Language
3. **區分同步與非同步** — 實線箭頭表示同步呼叫，虛線箭頭表示事件驅動
4. **Data Store 歸屬清晰** — 每個 Data Store 僅屬於一個 Bounded Context
5. **適度抽象** — 避免過度細化至程式碼層級，保持溝通價值

## 適用場景

- 新專案的架構文件撰寫
- 既有系統的資料流分析與視覺化
- 跨團隊溝通時的系統全貌說明
- 架構審查與技術債評估
