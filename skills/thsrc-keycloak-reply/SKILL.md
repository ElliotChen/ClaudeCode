---
name: thsrc-keycloak-reply
category: productivity
description: Process received THSRC KeyCloak permission review emails. Reads unread emails, copies/renames OneDrive files, creates To-Do tasks, marks emails complete, and moves them to ISO27001 folder.
version: 1.0.0
---

# THSRC KeyCloak 權限確認 Email 回覆處理

## Trigger
User asks to process/reply to received KeyCloak permission review emails, or handle unread emails in the "KeyCloak權限確認" folder.

## Folder Structure Convention
```
系統權限Review/
├── <系統名稱>/          (e.g., OP, BSM)
│   ├── <群組名稱>/       (e.g., GroupA, GroupB)
│   │   ├── Mail.md       # Contains 收件人 and 副本
│   │   └── <群組名稱>.xlsx  # Base file
│   │   └── <群組名稱>_YYYYQx.xlsx  # Historical versions
```

## Workflow: Processing Received Review Emails (Unread -> Done)

### Step 1: Connect and Open Outlook
```bash
playwright-cli attach --extension
playwright-cli goto "https://outlook.cloud.microsoft/mail"
playwright-cli snapshot
```
- Wait for "郵件 - elliot_chen (陳俊杰) - Outlook" title.

### Step 2: Find Unread Email in "KeyCloak權限確認"
- Click the folder: `playwright-cli click e<ref_of_folder>`
- Parse Subject: `.*: <系統> <年度> <季度> <群組> KeyCloak權限確認`
  - Example: `BSM 2026 Q2 GroupB KeyCloak權限確認`
  - Extract: System=BSM, Year=2026, Quarter=Q2, Group=GroupB.

### Step 3: Navigate to OneDrive
- Go to OneDrive: `playwright-cli goto "https://thsrc-my.sharepoint.com/my"`
- Click "My files" if needed.
- Navigate folders:
  1. Click `系統權限Review`
  2. Click `<系統名稱>` (e.g., BSM)
  3. Click `<群組名稱>` (e.g., GroupB)

### Step 4: Copy and Rename File
- Target: Copy `<群組>.xlsx` to `<群組>_<年度><季度>.xlsx` (e.g., `GroupB_2026Q2.xlsx`).
- **Check if target already exists**: If `<群組>_<年度><季度>.xlsx` already exists in the folder:
  - Check if the base `<群組>.xlsx` file exists in the same folder.
  - If base file DOES NOT exist: The target file was likely already created in a previous run. Skip the copy step entirely and proceed to Step 5 (Compare Files).
  - If base file EXISTS: Delete the existing `<群組>_<年度><季度>.xlsx` first, then proceed with the copy.
    - Select the existing `<群組>_<年度><季度>.xlsx` file.
    - Click "Delete" or press Delete key.
    - Wait for deletion to complete (verify it's gone from the list).
    - **Note**: If deletion fails with "file is open", dismiss the notification and skip the copy — the existing file is likely fine.
- Flow (only if base `<群組>.xlsx` exists):
  1. Select `<群組>.xlsx`.
  2. Click "Copy to".
  3. Click "Copy here" (in same folder).
  4. OneDrive creates `<群組>1.xlsx`.
  5. Click "Rename" -> Type `<群組>_<年度><季度>.xlsx` -> Update.

### Step 5: Compare Files
- Compare the new file with the previous quarter's file (e.g., `GroupB_2026Q1.xlsx`).
- Check file sizes or modification times as a heuristic.
- Record differences for the To Do task description.

### Step 6: Create To Do Task
- Navigate: `playwright-cli goto "https://to-do.office.com/tasks/?app"`
- Click "Keycloak" list in sidebar.
- Type task name: `<系統> <年度> <季度> <群組> KeyCloak 權限確認 - 檔案差異檢查`.
- Click "Add due date" -> "Pick a date" -> Select date (usually 1 week later) -> Save.
- Press Enter or click Add.

### Step 7: Mark Email as Complete and Move
- Go back to Outlook: `playwright-cli goto "https://outlook.cloud.microsoft/mail"`
- Open the "KeyCloak權限確認" folder.
- Open the email.
- Click "Flag" -> "展開以查看標幟選項" -> "標示完成" (Mark as complete).
- Verify the email no longer shows as unread/incomplete.
- **Move to "ISO27001" folder**:
  1. With the email still selected/open, click the **"移動" (Move)** button in the toolbar.
  2. Select **"ISO27001"** from the folder list. If not visible, click "移動至資料夾" (Move to folder) to open the dialog, search for "ISO27001", select it, and click OK.
  3. Verify the email disappears from the "KeyCloak權限確認" folder.

## Pitfalls
1. **Connection Stability**: `playwright-cli attach --extension` may drop. Re-run if disconnected.
2. **Snapshot Refs**: Refs change after every navigation. Always call `playwright-cli snapshot` after page changes.
3. **OneDrive Copy Naming**: Copying always creates `Filename1.ext`. You MUST rename it immediately.
4. **To Do Navigation**: Direct URL `https://to-do.office.com/tasks/?app` works best within the attached browser session.
5. **Excel Comparison**: Browser cannot easily diff Excel files. Rely on file metadata or user input for diff details.
6. **Marking Email Done**: The "Mark as complete" option is inside the Flag dropdown menu. Look for "標示完成".
7. **Existing Target File**: If `<群組>_<年度><季度>.xlsx` already exists (e.g. from a previous run), check if the base `<群組>.xlsx` still exists. If the base file is gone (consumed in a previous copy/rename), skip the copy step — the existing target file is already correct.
8. **playwright-cli type Fails with Spaces**: The `playwright-cli type` command may fail with "too many arguments" when typing text containing spaces or special characters. Use `playwright-cli fill <ref> "<text>"` instead for reliable text input.
9. **OneDrive Deletion "File Open" Error**: Attempting to delete a file in OneDrive may fail with "It looks like someone has the file open." Dismiss the notification and proceed — the file will typically delete on next refresh or can be skipped if it's not blocking the workflow.
10. **OneDrive REST API Available**: Within the playwright-cli attached session, you can use `fetch('/_api/v2.0/...')` via `playwright-cli eval` to list folder children and check file existence directly. This is faster than UI navigation. Use `const`/`let` (NOT `var`) — eval runs in strict mode.
11. **Post-Move Verification**: After moving to ISO27001, navigate to that folder and verify the email appears there. The KeyCloak權限確認 folder should show "資料夾中沒有項目" or the unread count should decrease.
