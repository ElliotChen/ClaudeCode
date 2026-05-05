# aors2 模組更新流程

廠商一次提供多個模組的 source code → 全部丟到 `patch/` → 一個版本號跑完整批 → 自動產生文件並同步到筆記目錄。

## 目錄結構

```
<workspace>/
├── modules.conf                模組 → category 對應表（必要）
├── patch/                      本輪要處理的廠商 source（每個模組一個子目錄）
│   ├── biz-management/
│   ├── device-management/
│   └── ...
├── gitrepo/                    累積的 git 倉庫
│   ├── infra/<module>/
│   ├── ams/<module>/
│   ├── dms/<module>/
│   └── web/<module>/
│       ├── (source code)
│       ├── .aorskeep           （可選）此模組需保留的自訂檔案清單
│       └── docs/
│           ├── commit/
│           ├── error/
│           └── schedule/
├── scripts/
│   ├── lib.sh                  共用函式
│   ├── apply-patch.sh
│   ├── generate-docs.sh
│   ├── sync-docs.sh
│   ├── update.sh               一鍵批次處理
│   └── .aorskeep.example
└── ~/Documents/note/aors2/     文件彙整目錄（由 sync-docs.sh 同步）
    ├── infra/<module>/...
    ├── ams/<module>/...
    └── ...
```

## modules.conf

設定檔放在 workspace 根目錄。一個模組一行：

```
# 格式：<module>:<category>
biz-management:ams
order-management:ams
device-management:dms
gateway:infra
admin-portal:web
```

`category` 必須是 `infra` / `ams` / `dms` / `web` 之一。
範本檔：`modules.conf.example`，第一次使用時複製為 `modules.conf` 並依實際模組調整。

新增模組時：先 `git clone` 進 `gitrepo/<cat>/<module>/`，再加一行到 `modules.conf`。

## 日常流程

廠商給一批 source code 後：

```bash
# 1. 把廠商每個模組的 source 放進 patch/ 對應子目錄
cp -r /path/from/vendor/biz-management     ./patch/
cp -r /path/from/vendor/device-management  ./patch/
cp -r /path/from/vendor/gateway            ./patch/

# 2. 一鍵批次處理（同一個版本號）
./scripts/update.sh 1.6.10
```

`update.sh` 會：

1. 掃描 `patch/` 下所有模組，查 `modules.conf` 確認 category。
2. 顯示本輪要處理的清單，請您確認。
3. 對每個模組：套 patch → `git commit -m 'release/1.6.10'`。
4. 對每個模組依序呼叫 skill（`commit-doc-generator`、`error-doc-generator`、`schedule-doc-generator`）。
5. 把本輪模組的 `docs/` 同步到 `~/Documents/note/aors2/<cat>/<module>/`。
6. 詢問是否清除 `patch/` 內這幾個模組目錄。

整批共用 `release/1.6.10` 這個版本訊息。

## 個別腳本

```bash
# 套 patch + commit（patch/ 下所有模組）
./scripts/apply-patch.sh 1.6.10

# 只處理單一模組
./scripts/apply-patch.sh 1.6.10 biz-management

# 產文件（patch/ 下所有模組）
./scripts/generate-docs.sh 1.6.10

# 只對單一模組產文件
./scripts/generate-docs.sh 1.6.10 biz-management

# 同步本輪 patch/ 模組的 docs 到 note
./scripts/sync-docs.sh --patch

# 同步單一模組
./scripts/sync-docs.sh biz-management

# 同步 modules.conf 內所有已建立 repo 的模組
./scripts/sync-docs.sh
```

## 自訂保留檔案：`.aorskeep`

廠商的 patch 會用 `rsync --delete` 套用，廠商 source 中沒有的檔案會被清掉。
如果模組裡有「您自己加的、不希望被清掉」的檔案，把路徑寫在該模組根目錄的 `.aorskeep`：

```
# gitrepo/ams/biz-management/.aorskeep
README.md
CHANGELOG.local.md
.gitignore
.vscode/
```

預設一定保留：`.git/`、`docs/`、`.aorskeep`，不必重複寫。
語法同 rsync 的 exclude pattern。`scripts/.aorskeep.example` 是範本。

## 自訂 skill 清單

`generate-docs.sh` 中的 `SKILLS` 陣列就是會跑的 skill，按順序執行。
要加新文件種類（例如 `api-doc-generator`）直接加到陣列即可。

## 環境變數

- `AORS_NOTE_DIR` — 覆寫筆記目錄位置（預設 `~/Documents/note/aors2`）
- `MODULES_CONF` — 覆寫設定檔位置（預設 `<workspace>/modules.conf`）

## 安裝

```bash
chmod +x scripts/*.sh
cp modules.conf.example modules.conf
# 然後編輯 modules.conf，填入實際模組
```

需求：bash、rsync、git、claude（Claude Code CLI）。
