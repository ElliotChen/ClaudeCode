# API curl 範例

本文檔提供 Employee Management API 的 curl 使用範例。

- **Base URL**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

## 通用 Headers

所有請求建議包含以下 headers：

```bash
-H "Content-Type: application/json" \
-H "X-Trace-Id: <unique-trace-id>"
```

`X-Trace-Id` 用於追蹤請求，便於日誌查詢與除錯。

---

## 1. 雇用員工

**Endpoint:** `POST /api/employees`

**curl 範例:**

```bash
curl -X POST "http://localhost:8080/api/employees?name=王小明&departmentId=550e8400-e29b-41d4-a716-446655440001" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-001-hire"
```

**回應範例 (200 OK):**

```json
{
  "id": "0192a1b2-c3d4-e5f6-7890-abcdef123456",
  "name": "王小明",
  "rank": "NORMAL",
  "status": "ACTIVE",
  "departmentId": "550e8400-e29b-41d4-a716-446655440001",
  "createdAt": "2026-04-08T10:00:00Z",
  "updatedAt": "2026-04-08T10:00:00Z"
}
```

**回應欄位說明:**

| 欄位 | 說明 |
|------|------|
| id | 員工 UUID |
| name | 員工姓名 |
| rank | 職級 (NORMAL, LEAD, MANAGER) |
| status | 狀態 (ACTIVE, INACTIVE) |
| departmentId | 所屬部門 UUID |
| createdAt | 建立時間 |
| updatedAt | 更新時間 |

---

## 2. 解雇員工

**Endpoint:** `DELETE /api/employees/{id}`

**curl 範例:**

```bash
curl -X DELETE "http://localhost:8080/api/employees/0192a1b2-c3d4-e5f6-7890-abcdef123456" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-002-fire"
```

**回應範例 (204 No Content):**

```
(無內容)
```

**說明:** 成功後將員工狀態設為 `INACTIVE` 並移除部門關聯。

---

## 3. 調職

**Endpoint:** `POST /api/employees/{id}/transfer`

**curl 範例:**

```bash
curl -X POST "http://localhost:8080/api/employees/0192a1b2-c3d4-e5f6-7890-abcdef123456/transfer?targetDepartmentId=550e8400-e29b-41d4-a716-446655440002" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-003-transfer"
```

**回應範例 (204 No Content):**

```
(無內容)
```

**說明:** 將員工調動至目標部門。

---

## 4. 升職

**Endpoint:** `POST /api/employees/{id}/promote`

**curl 範例:**

```bash
curl -X POST "http://localhost:8080/api/employees/0192a1b2-c3d4-e5f6-7890-abcdef123456/promote" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-004-promote"
```

**回應範例 (204 No Content):**

```
(無內容)
```

**說明:** 升職路徑：`NORMAL` → `LEAD` → `MANAGER`

---

## 5. 降級

**Endpoint:** `POST /api/employees/{id}/demote`

**curl 範例:**

```bash
curl -X POST "http://localhost:8080/api/employees/0192a1b2-c3d4-e5f6-7890-abcdef123456/demote" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-005-demote"
```

**回應範例 (204 No Content):**

```
(無內容)
```

**說明:** 降級路徑：`MANAGER` → `LEAD` → `NORMAL`

---

## 6. 查詢員工

**Endpoint:** `GET /api/employees/{id}`

**curl 範例:**

```bash
curl -X GET "http://localhost:8080/api/employees/0192a1b2-c3d4-e5f6-7890-abcdef123456" \
  -H "X-Trace-Id: trace-006-get-employee"
```

**回應範例 (200 OK):**

```json
{
  "id": "0192a1b2-c3d4-e5f6-7890-abcdef123456",
  "name": "王小明",
  "rank": "LEAD",
  "status": "ACTIVE",
  "departmentId": "550e8400-e29b-41d4-a716-446655440001",
  "createdAt": "2026-04-01T08:00:00Z",
  "updatedAt": "2026-04-08T14:30:00Z"
}
```

---

## 7. 建立部門

**Endpoint:** `POST /api/departments`

**curl 範例:**

```bash
curl -X POST "http://localhost:8080/api/departments?name=工程部" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-007-create-dept"
```

**回應範例 (201 Created):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "工程部",
  "createdAt": "2026-04-08T10:00:00Z",
  "updatedAt": "2026-04-08T10:00:00Z"
}
```

**說明:** 回應包含 `Location` header 指向新建立的部門資源。

---

## 8. 查詢部門

**Endpoint:** `GET /api/departments/{id}`

**curl 範例:**

```bash
curl -X GET "http://localhost:8080/api/departments/550e8400-e29b-41d4-a716-446655440001" \
  -H "X-Trace-Id: trace-008-get-dept"
```

**回應範例 (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "工程部",
  "createdAt": "2026-04-01T08:00:00Z",
  "updatedAt": "2026-04-08T10:00:00Z"
}
```

---

## 9. 查詢部門員工

**Endpoint:** `GET /api/departments/{id}/employees`

**curl 範例:**

```bash
curl -X GET "http://localhost:8080/api/departments/550e8400-e29b-41d4-a716-446655440001/employees" \
  -H "X-Trace-Id: trace-009-get-dept-employees"
```

**回應範例 (200 OK):**

```json
[
  {
    "id": "0192a1b2-c3d4-e5f6-7890-abcdef123456",
    "name": "王小明",
    "rank": "LEAD",
    "status": "ACTIVE",
    "departmentId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-04-01T08:00:00Z",
    "updatedAt": "2026-04-08T14:30:00Z"
  },
  {
    "id": "0192a1b2-c3d4-e5f6-7890-abcdef123457",
    "name": "李小華",
    "rank": "NORMAL",
    "status": "ACTIVE",
    "departmentId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-04-02T09:00:00Z",
    "updatedAt": "2026-04-02T09:00:00Z"
  }
]
```

---

## 10. 查詢異動歷史

**Endpoint:** `GET /api/hr/{employeeId}/history`

**curl 範例:**

```bash
curl -X GET "http://localhost:8080/api/hr/0192a1b2-c3d4-e5f6-7890-abcdef123456/history" \
  -H "X-Trace-Id: trace-010-get-hr-history"
```

**回應範例 (200 OK):**

```json
[
  {
    "id": "660f9500-f39c-52e5-b827-557766551101",
    "employeeId": "0192a1b2-c3d4-e5f6-7890-abcdef123456",
    "actionType": "HIRED",
    "oldValue": null,
    "newValue": {
      "name": "王小明",
      "rank": "NORMAL",
      "status": "ACTIVE"
    },
    "createdAt": "2026-04-01T08:00:00Z",
    "recordedAt": "2026-04-01T08:00:00Z"
  },
  {
    "id": "660f9500-f39c-52e5-b827-557766551102",
    "employeeId": "0192a1b2-c3d4-e5f6-7890-abcdef123456",
    "actionType": "PROMOTED",
    "oldValue": {
      "rank": "NORMAL"
    },
    "newValue": {
      "rank": "LEAD"
    },
    "createdAt": "2026-04-05T11:00:00Z",
    "recordedAt": "2026-04-05T11:00:00Z"
  },
  {
    "id": "660f9500-f39c-52e5-b827-557766551103",
    "employeeId": "0192a1b2-c3d4-e5f6-7890-abcdef123456",
    "actionType": "TRANSFERRED",
    "oldValue": {
      "departmentId": "550e8400-e29b-41d4-a716-446655440000"
    },
    "newValue": {
      "departmentId": "550e8400-e29b-41d4-a716-446655440001"
    },
    "createdAt": "2026-04-08T14:30:00Z",
    "recordedAt": "2026-04-08T14:30:00Z"
  }
]
```

**actionType 說明:**

| 值 | 說明 |
|----|------|
| HIRED | 雇用 |
| FIRED | 解雇 |
| TRANSFERRED | 調職 |
| PROMOTED | 升職 |
| DEMOTED | 降級 |

---

## 錯誤回應範例

**404 Not Found:**

```json
{
  "timestamp": "2026-04-08T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/employees/0192a1b2-c3d4-e5f6-7890-abcdef123456"
}
```

**400 Bad Request:**

```json
{
  "timestamp": "2026-04-08T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Required parameter 'name' is not present",
  "path": "/api/employees"
}
```

**500 Internal Server Error:**

```json
{
  "timestamp": "2026-04-08T10:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/employees"
}
```

---

## 快速測試腳本

以下為 Bash 腳本，快速測試所有 API：

```bash
#!/bin/bash
BASE_URL="http://localhost:8080/api"
TRACE_ID="test-$(date +%s)"

# 建立部門
echo "=== 建立部門 ==="
DEPT_RESP=$(curl -s -X POST "$BASE_URL/departments?name=測試部門" \
  -H "X-Trace-Id: $TRACE_ID-dept")
echo $DEPT_RESP
DEPT_ID=$(echo $DEPT_RESP | jq -r '.id')

# 雇用員工
echo -e "\n=== 雇用員工 ==="
EMP_RESP=$(curl -s -X POST "$BASE_URL/employees?name=測試員工&departmentId=$DEPT_ID" \
  -H "X-Trace-Id: $TRACE_ID-hire")
echo $EMP_RESP
EMP_ID=$(echo $EMP_RESP | jq -r '.id')

# 查詢員工
echo -e "\n=== 查詢員工 ==="
curl -s -X GET "$BASE_URL/employees/$EMP_ID" \
  -H "X-Trace-Id: $TRACE_ID-get" | jq .

# 升職
echo -e "\n=== 升職 ==="
curl -s -X POST "$BASE_URL/employees/$EMP_ID/promote" \
  -H "X-Trace-Id: $TRACE_ID-promote" -w "\nHTTP Status: %{http_code}\n"

# 查詢異動歷史
echo -e "\n=== 異動歷史 ==="
curl -s -X GET "$BASE_URL/hr/$EMP_ID/history" \
  -H "X-Trace-Id: $TRACE_ID-history" | jq .

# 查詢部門員工
echo -e "\n=== 部門員工 ==="
curl -s -X GET "$BASE_URL/departments/$DEPT_ID/employees" \
  -H "X-Trace-Id: $TRACE_ID-dept-emp" | jq .

# 解雇
echo -e "\n=== 解雇 ==="
curl -s -X DELETE "$BASE_URL/employees/$EMP_ID" \
  -H "X-Trace-Id: $TRACE_ID-fire" -w "HTTP Status: %{http_code}\n"
```
