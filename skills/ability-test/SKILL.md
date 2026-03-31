---
name: ability-test
description: 為了驗證AI當前實際能力，將常用的測試項目表列以進行驗證。
---

# AI能力驗證

為了驗證AI當前實際能力，將常用的測試項目表列以進行驗證。當執行此Skill時，需要詢問使用者要進行以下哪種驗證

## 1. [File]多檔案讀取
讀取'workspace/ability/docs'目錄下所有檔案，並以檔案的內容各別向使用者簡單說明

## 2. [File]比對內容
讀取'workspace/ability/diff'目錄最後兩個版本檔案，比對內容差異後說明

## 3. [Git]Commit 說明
若使用者未列出明確的commit或tag時，列出最近的5個git commit供使用者選擇，確認commit的number後，說明該commit所變更的內容與可能𠩤因

## 4. [File][Create]List Todo List
計劃如何分析當前目錄Context，但不要真的執行，將要執行的步驟列為todo list存於'workspace/ability/todo’目錄下。

## 5. [File][Modify]Execute Todo List
讀取'workspace/ability/todo’目錄下檔案，執行第一個未完全的項目，完成後就更新狀態。

## 6. [Tool][Search] Maven Library Version Retrieve
以可用工具取得’org.springframework.boot’最後的穩定版本，並將執行過程揭示給使用者。

## 7. [Tool][Search] Fix Maven Library Version Retrieve
以可用工具取得’org.springframework.boot’最後的穩定版本，並將執行過程揭示給使用者。
查詢 Maven 套件版本時，一律使用 central.sonatype.com，不使用 search.maven.org，並使用'sort=v+desc’做為條件，範例為'https://central.sonatype.com/solrsearch/select?q=g:<gropup_id>+AND+a:<artifact_id>&wt=json&core=gav&sort=v+desc' ，不可使用 *search.maven.org*，因其已不再更新。