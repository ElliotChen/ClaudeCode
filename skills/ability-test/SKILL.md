---
name: ability-test
description: 為了驗證AI當前實際能力，將常用的測試項目表列以進行驗證。
---

# AI能力驗證

為了驗證AI當前實際能力，將常用的測試項目表列以進行驗證。當執行此Skill時，需要詢問使用者要進行以下哪種驗證

## 1. 各別檔案讀取
讀取'workspace/ability/docs'目錄下所有檔案，並各別為檔案進行內容簡單說明

## 2. 比對內容
讀取'workspace/ability/diff'目錄最後兩個版本檔案，比對內容差異後說明

## 3. Commit 說明
若使用者未列出明確的commit或tag時，列出最近的5個git commit供使用者選擇，確認commit的number後，說明該commit所變更的內容與可能𠩤因
