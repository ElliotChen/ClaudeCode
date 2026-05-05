---
name: thsrc-keycloak-remind
category: productivity
description: Send new THSRC KeyCloak permission review reminder emails based on OneDrive folder structure. Checks To-Do for trigger, extracts Mail.md info and file links via REST API, then composes and sends batch emails.
version: 1.0.0
---

# THSRC KeyCloak 權限確認 Email 寄送提醒

## Trigger
User asks to send KeyCloak permission review reminder emails, or automate the quarterly KeyCloak review notification workflow.

## Folder Structure Convention
```
系統權限Review/
├── <系統名稱>/          (e.g., OP, BSM)
│   ├── <群組名稱>/       (e.g., GroupA, GroupB)
│   │   ├── Mail.md       # Contains 收件人 and 副本
│   │   └── <群組名稱>.xlsx  # Base file
│   │   └── <群組名稱>_YYYYQx.xlsx  # Historical versions
```

## Workflow: Sending New Review Emails (From Folder Structure)

### Pre-flight Check
1. Navigate: `playwright-cli goto "https://to-do.office.com/tasks/?app"`
2. Click "Keycloak" list in sidebar.
3. If not found, stop immediately and inform the user.

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
- Fill 收件人/副本 (Note: external emails in CC may require special handling — see Pitfall 4).
- Subject: `<系統> <年度> <季度> <群組> KeyCloak權限確認`
- Body: Include link to the **base** `<群組>.xlsx` file (e.g., `GroupB.xlsx`).
  - **IMPORTANT**: Always use the base file link (檔名不含年度/季度), regardless of whether a quarterly version (e.g., `GroupB_2026Q2.xlsx`) exists or not. Do NOT check for or link the quarterly file when sending review emails.

### Step 4: Verify Delivery
- After sending each email, verify the draft count in the Outlook sidebar decreases by 1 to confirm successful delivery.
- Baseline draft count: typically 11. After sending N emails, expected: 11 - N.

## Pitfalls
1. **Connection Stability**: `playwright-cli attach --extension` may drop. Re-run if disconnected.
2. **Snapshot Refs**: Refs change after every navigation. Always call `playwright-cli snapshot` after page changes.
3. **CC External Emails**: Outlook web CC field opens address book dialog when clicked. Working workaround:
   1. Click the CC button to activate the field
   2. If address book dialog opens, click "Cancel"
   3. Use `playwright-cli fill <cc_group_ref> "<email>"` on the CC group element
   4. Click the suggested result option (appears as `option "<email> - <email>"` in suggestion listbox) to confirm
   5. The external email will appear in the CC field with "外部" label
4. **To Do Navigation**: Direct URL `https://to-do.office.com/tasks/?app` works best within the attached browser session.
5. **playwright-cli type Fails with Spaces**: The `playwright-cli type` command may fail with "too many arguments" when typing text containing spaces or special characters. Use `playwright-cli fill <ref> "<text>"` instead for reliable text input.
6. **playwright-cli eval JavaScript Limitation**: The eval command runs in strict mode and does NOT support `var` declarations. Use direct expressions, arrow functions, or `const`/`let` instead. Example that works: `document.querySelector('[id*=\'_CC\']') ? 'found' : 'not found'`. Example that fails: `var x = document.querySelector(...)`
7. **OneDrive REST API Available**: Within the playwright-cli attached session, you can use `fetch('/_api/v2.0/...')` to read Mail.md content, list folder children, and get file webUrls directly. This is much faster and more reliable than UI navigation for batch operations. The API responses include `webUrl` for file sharing links, `childCount` for folder info, and direct text content for files via `:/content` endpoint.
8. **Batch Email Workflow**: When sending multiple review emails, collect all info via REST API first (folder structure, Mail.md contents, file URLs), then compose and send emails in sequence. This avoids repeated UI navigation between OneDrive and Outlook.
9. **Draft Count Verification**: After sending an email, verify the draft count in the sidebar decreases by 1 to confirm successful delivery.
10. **Base File Link Enforcement**: When composing emails, the email body must **always** link to the base `<群組>.xlsx` file. Do NOT check for or link to quarterly versions (`<群組>_YYYYQx.xlsx`).
