---
name: thsrc-keycloak-permission-review
category: productivity
description: Automate THSRC KeyCloak permission review workflow. Includes processing unread KeyCloak emails (Navigate OneDrive -> Copy/Rename File -> Compare -> Create To Do -> Mark Email Done) and sending new review emails based on folder structure.
version: 2.1.0
---

# THSRC KeyCloak 權限確認 Email 自動化

## Trigger
User asks to process KeyCloak permission review emails or automate the quarterly KeyCloak review workflow.

## Workflow Overview
This skill covers two main flows:
1. **Processing Received Review Emails**: Reading email -> OneDrive file copy -> Create To Do -> Mark email done.
2. **Sending New Review Emails**: Based on folder structure (Legacy flow).

## Folder Structure Convention
```
系統權限Review/
├── <系統名稱>/          (e.g., OP, BSM)
│   ├── <群組名稱>/       (e.g., GroupA, GroupB)
│   │   ├── Mail.md       # Contains 收件人 and 副本
│   │   └── <群組名稱>.xlsx  # Base file
│   │   └── <群組名稱>_YYYYQx.xlsx  # Historical versions
```

## Part 1: Processing Received Review Emails (Unread -> Done)

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
- Click “Keycloak" list in sidebar.
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
  1. With the email still selected/open, click the **"移動" (Move)** button in the toolbar (or right-click > Move).
  2. Select **"ISO27001"** from the folder list. If not visible, click "移動至資料夾" (Move to folder) to open the dialog, search for "ISO27001", select it, and click OK.
  3. Verify the email disappears from the "KeyCloak權限確認" folder.

## Part 2: Sending New Review Emails (From Folder Structure)

### Pre-flight Check
1. Navigate: `playwright-cli goto "https://to-do.office.com/tasks/?app"`
2. Click "Keycloak" list in sidebar.
3. Check for a task named "keycloak-permission-review" in the "Keycloak" category due today.
4. If not found, stop immediately and inform the user.

### Step 1: Explore Folders
- **Preferred**: Use OneDrive REST API to discover folders and files:
  ```javascript
  // List top-level systems under 系統權限Review
  fetch('/_api/v2.0/drives/me/root:/系統權限Review:/children')
    .then(r => r.json()).then(j => j.value.filter(c => c.folder).map(c => c.name))
  
  // List groups under a system
  fetch('/_api/v2.0/drives/me/root:/系統權限Review/<系統>:/children')
    .then(r => r.json()).then(j => j.value.filter(c => c.folder).map(c => c.name))
  ```
- **Fallback**: Navigate via UI: Go to OneDrive -> Click `系統權限Review` -> Identify all `<系統>/<群組>` folders.

### Step 2: Extract Info and Get Share Links (OneDrive REST API preferred)
- **Preferred**: Use OneDrive REST API via `playwright-cli eval` to read Mail.md and get file links in one pass — do NOT skip the file link step or come back later:
  ```javascript
  // Read Mail.md content
  fetch('/_api/v2.0/drives/me/root:/系統權限Review/<系統>/<群組>/Mail.md:/content')
    .then(r => r.text())
    .then(t => console.log(t))
  // Returns: "## Mail Receiver\n\n收件人:\n- email@domain.com\n\n副本:\n- other@email.com"

  // Get the BASE file webUrl for email body — do this NOW during folder scan, do NOT defer
  // IMPORTANT: Always use the base file (<群組>.xlsx), NOT the quarterly version (<群組>_YYYYQx.xlsx)
  fetch('/_api/v2.0/drives/me/root:/系統權限Review/<系統>/<群組>/<群組>.xlsx')
    .then(r => r.json())
    .then(j => j.webUrl)

  // List folder contents (optional: check which files exist)
  fetch('/_api/v2.0/drives/me/root:/系統權限Review/<系統>/<群組>:/children')
    .then(r => r.json())
    .then(j => j.value.map(c => c.name))
  ```
- **Fallback**: Click `Mail.md` to preview in UI. Note: Mail.md preview may render blank in browser. If so, use the API approach above.
- For each `<系統>/<群組>`, collect a complete record: `{系統, 群組, 收件人, 副本, 基本檔連結}`. **Do not proceed to Step 3 until all records have their file links.**
- **IMPORTANT**: Always use the base file link (檔名不含年度/季度), e.g., `GroupB.xlsx`, regardless of whether a quarterly version (e.g., `GroupB_2026Q2.xlsx`) exists. Do NOT analyze or link the quarterly file when sending review emails.

### Step 3: Send Email
- Navigate to Outlook.
- Compose new email.
- Fill 收件人/副本 (Note: external emails in CC may require special handling or API).
- Subject: `<系統> <年度> <季度> <群組> KeyCloak權限確認`
- Body: Include link to the **base** `<群組>.xlsx` file (e.g., `GroupB.xlsx`).
  - **IMPORTANT**: Always use the base file link (檔名不含年度/季度), regardless of whether a quarterly version (e.g., `GroupB_2026Q2.xlsx`) exists or not. Do NOT check for or link the quarterly file when sending review emails.

## Pitfalls
1. **Connection Stability**: `playwright-cli attach --extension` may drop. Re-run if disconnected.
2. **Snapshot Refs**: Refs change after every navigation. Always call `playwright-cli snapshot` after page changes.
3. **OneDrive Copy Naming**: Copying always creates `Filename1.ext`. You MUST rename it immediately.
4. **CC External Emails**: Outlook web CC field opens address book dialog when clicked. Working workaround:
   1. Click the CC button to activate the field
   2. If address book dialog opens, click "Cancel"
   3. Use `playwright-cli fill <cc_group_ref> "<email>"` on the CC group element
   4. Click the suggested result option (appears as `option "<email> - <email>"` in suggestion listbox) to confirm
   5. The external email will appear in the CC field with "外部" label
5. **To Do Navigation**: Direct URL `https://to-do.office.com/tasks/?app` works best within the attached browser session.
6. **Excel Comparison**: Browser cannot easily diff Excel files. Rely on file metadata or user input for diff details.
7. **Marking Email Done**: The "Mark as complete" option is inside the Flag dropdown menu. Look for "標示完成".
8. **Existing Target File**: If `<群組>_<年度><季度>.xlsx` already exists (e.g. from a previous run), check if the base `<群組>.xlsx` still exists. If the base file is gone (consumed in a previous copy/rename), skip the copy step — the existing target file is already correct.
9. **playwright-cli type Fails with Spaces**: The `playwright-cli type` command may fail with "too many arguments" when typing text containing spaces or special characters. Use `playwright-cli fill <ref> "<text>"` instead for reliable text input.
10. **OneDrive Deletion "File Open" Error**: Attempting to delete a file in OneDrive may fail with "It looks like someone has the file open." Dismiss the notification and proceed — the file will typically delete on next refresh or can be skipped if it's not blocking the workflow.
11. **OneDrive REST API Available**: Within the playwright-cli attached session, you can use `fetch('/_api/v2.0/...')` to read Mail.md content, list folder children, and get file webUrls directly. This is much faster and more reliable than UI navigation for batch operations. The API responses include `webUrl` for file sharing links, `childCount` for folder info, and direct text content for files via `:/content` endpoint.
12. **Batch Email Workflow**: When sending multiple review emails (Part 2), collect all info via REST API first (folder structure, Mail.md contents, file URLs), then compose and send emails in sequence. This avoids repeated UI navigation between OneDrive and Outlook.
13. **playwright-cli eval JavaScript Limitation**: The eval command runs in strict mode and does NOT support `var` declarations. Use direct expressions, arrow functions, or `const`/`let` instead. Example that works: `document.querySelector('[id*=\'_CC\']') ? 'found' : 'not found'`. Example that fails: `var x = document.querySelector(...)`
14. **Draft Count Verification**: After sending an email, verify the draft count in the sidebar decreases by 1 to confirm successful delivery.