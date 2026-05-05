---
name: commit-doc-generator
description: 分析 git commit 變更，依模組產出繁體中文分析文件。當使用者要求分析 commit、產生 commit 文件、或檢視 commit 變更摘要時觸發。
---

# Commit 分析文件生成

分析指定的 git commit，依頂層模組逐一歸納變更問題，產出結構化的繁體中文分析文件至 `docs/commit/<short-hash>.md`。

## 流程

### 第一步：列出 Commits 供選擇

使用 Bash 工具執行以下指令，列出最近 10 筆 commit：

```bash
git log --oneline --format="%h %s (%ai)" -10
```

將結果以編號清單呈現給使用者，並提示：

> 請選擇要分析的 commit：
> - 輸入 commit hash 來分析該筆
> - 直接按 Enter（不輸入）分析最新一筆
> - 輸入 `more` 查看更多 commits

若使用者輸入 `more`，增加顯示數量（改為 `-20`、`-30`⋯），重新列出後再次詢問。

若使用者不輸入值，使用清單中第一筆（最新的）commit。

### 第二步：取得 Commit 基本資訊

確認目標 commit hash 後，執行以下指令取得基本資訊：

```bash
git show --no-patch --format="hash:%H%nshort:%h%nsubject:%s%nauthor:%an%ndate:%ai" <hash>
```

記錄以下欄位供後續使用：
- `full-hash`：完整 hash
- `short-hash`：短 hash
- `subject`：commit message
- `author`：提交者
- `date`：提交日期

### 第三步：取得變更檔案清單並分組

執行以下指令取得變更檔案清單：

```bash
git show --name-only --format="" <hash>
```

將檔案依第一層目錄分組為模組。例如：
- `reservation/src/main/java/Foo.java` → 模組 `reservation`
- `aors-infra/deploy/dev.yaml` → 模組 `aors-infra`
- `build.gradle` → 模組「專案根目錄」

只保留在此 commit 中有變更的模組。

### 第四步：逐模組分析

對每個有變更的模組，使用 Bash 工具執行以下指令取得該模組的 diff：

```bash
git show <hash> -- <module>/
```

對於「專案根目錄」的檔案，逐一取得 diff：

```bash
git show <hash> -- <filename>
```
Diff內容需**全部**讀取，不可以head參數略過。


根據 diff 內容分析：
根據程式碼變更推斷修改的原因與問題背景，
若同一模組內的變更可歸納為**多個獨立問題**，需要**分別列出**，每個問題自己的「問題背景與解決方案」和「變更摘要」。
列出的說明應參考下列格式與說明
1. **問題背景與解決方案**：根據程式碼變更推斷修改的原因與問題背景，說明問題本身，背景與情境，可能造成的結果，最後採取了什麼解決方案。需詳細描述，讓不熟悉此模組的人也能理解。
2. **變更摘要**：
   - **實作說明**：簡述具體改了哪些類別/方法/配置，做了什麼修改
   - **業務用途**：這個變更對業務流程的意義
   - **影響範圍**：可能影響的上下游模組或功能
3. **參數新增／變更**：
   列出新增的參數名稱與值
   變更的部分，則僅列出會影響業務邏輯相關的名稱與值

### 第五步：生成文件

確保輸出目錄存在：

```bash
mkdir -p docs/commit
```

使用 Write 工具將分析結果寫入 `docs/commit/<commit-message>_<short-hash>.md`，格式如下：

```
# Commit 分析：<short-hash>

- **提交訊息：** <subject>
- **提交者：** <author>
- **提交日期：** <date>

## <模組名稱>

### 問題背景與解決方案1

<修改的問題背景，以及採取的解決方案>

### 變更摘要

- **實作說明：** <簡述具體改了什麼>
- **業務用途：** <這個變更對業務的意義>
- **影響範圍：** <可能影響的上下游模組或功能>

### 問題背景與解決方案2

<修改的問題背景，以及採取的解決方案>

### 變更摘要

- **實作說明：** <簡述具體改了什麼>
- **業務用途：** <這個變更對業務的意義>
- **影響範圍：** <可能影響的上下游模組或功能>
---
```

每個有變更的模組重複以上結構。模組之間以 `---` 分隔。

### 第六步：回報結果

告知使用者文件已生成，並顯示檔案路徑：

> 分析文件已生成：`docs/commit/<commit-message>_<short-hash>.md`

## 注意事項

- 所有輸出皆為繁體中文
- 若模組變更僅涉及 YAML/properties 等配置檔，在問題背景中標注為「配置變更」
- 分析應基於 diff 內容推斷，而非猜測
- 若 diff 過大導致無法一次讀取，分批取得各模組的 diff
