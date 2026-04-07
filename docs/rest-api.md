# REST API Documentation

**Base URL:** `http://localhost:8081`

---

## Table of Contents

- [Dashboard](#dashboard)
- [Customers](#customers)
- [Employees](#employees)
- [Companies](#companies)
- [Accounts](#accounts)
- [Customer-Account Links](#customer-account-links)
- [Zeebe Connection & Worker Status](#zeebe-connection--worker-status)
- [Job History & Metrics](#job-history--metrics)
- [Engine Events](#engine-events)
- [Engine Event Presets](#engine-event-presets)
- [Engine Event Dispatches](#engine-event-dispatches)
- [Data Models Reference](#data-models-reference)

---

## Dashboard

### `GET /`

Returns the HTML dashboard page (Thymeleaf template).

### `GET /favicon.ico`

Returns `204 No Content`. Prevents browser 404 errors for the favicon.

---

## Customers

### `GET /api/customers`

Returns all customers.

**Response:** `200 OK`
```json
[
  {
    "customerId": 1,
    "customerName": "Jane Doe",
    "address": "123 Main St",
    "email": "jane@example.com",
    "employeeId": 10,
    "trustLevel": "L2",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
]
```

### `GET /api/customers/{id}`

Returns a single customer by ID.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Customer ID |

**Response:** `200 OK` — Customer object (see above).

### `POST /api/customers`

Creates a new customer. The request body is validated.

**Request Body:**
```json
{
  "customerName": "Jane Doe",
  "address": "123 Main St",
  "email": "jane@example.com",
  "employeeId": 10,
  "trustLevel": "L2"
}
```

| Field          | Type   | Required | Description                              |
|----------------|--------|----------|------------------------------------------|
| `customerName` | String | Yes      | Customer's full name                     |
| `address`      | String | Yes      | Customer's address                       |
| `email`        | String | Yes      | Must be a valid email; unique constraint |
| `employeeId`   | Long   | Yes      | Must be positive; references an Employee |
| `trustLevel`   | String | Yes      | One of: `L1`, `L2`, `L3`                |

**Response:** `200 OK` — The created Customer object.

### `PUT /api/customers/{id}`

Updates an existing customer. The path `id` overrides any ID in the body.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Customer ID |

**Request Body:** Same as POST.

**Response:** `200 OK` — The updated Customer object.

### `DELETE /api/customers/{id}`

Deletes a customer by ID.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Customer ID |

**Response:** `200 OK`
```json
{
  "message": "Customer deleted successfully"
}
```

---

## Employees

### `GET /api/employees`

Returns all employees.

**Response:** `200 OK`
```json
[
  {
    "employeeId": 10,
    "fullName": "John Smith",
    "jobTitle": "Account Manager",
    "department": "Sales",
    "phoneNumber": "+1-555-0100",
    "createdAt": "2025-01-10T08:00:00",
    "updatedAt": "2025-01-10T08:00:00"
  }
]
```

### `GET /api/employees/{id}`

Returns a single employee by ID.

| Parameter | Type | Location | Description  |
|-----------|------|----------|--------------|
| `id`      | Long | Path     | Employee ID  |

**Response:** `200 OK` — Employee object (see above).

### `POST /api/employees`

Creates a new employee. The request body is validated.

**Request Body:**
```json
{
  "fullName": "John Smith",
  "jobTitle": "Account Manager",
  "department": "Sales",
  "phoneNumber": "+1-555-0100"
}
```

| Field         | Type   | Required | Description          |
|---------------|--------|----------|----------------------|
| `fullName`    | String | Yes      | Employee's full name |
| `jobTitle`    | String | Yes      | Job title            |
| `department`  | String | Yes      | Department name      |
| `phoneNumber` | String | No       | Contact number       |

**Response:** `200 OK` — The created Employee object.

### `PUT /api/employees/{id}`

Updates an existing employee. The path `id` overrides any ID in the body.

| Parameter | Type | Location | Description  |
|-----------|------|----------|--------------|
| `id`      | Long | Path     | Employee ID  |

**Request Body:** Same as POST.

**Response:** `200 OK` — The updated Employee object.

### `DELETE /api/employees/{id}`

Deletes an employee by ID.

| Parameter | Type | Location | Description  |
|-----------|------|----------|--------------|
| `id`      | Long | Path     | Employee ID  |

**Response:** `200 OK`
```json
{
  "message": "Employee deleted successfully"
}
```

---

## Companies

### `GET /api/companies`

Returns all external companies.

**Response:** `200 OK`
```json
[
  {
    "companyId": 1,
    "companyName": "Acme Corp",
    "address": "456 Business Ave",
    "contactPerson": "Bob Jones",
    "phoneNumber": "+1-555-0200",
    "createdAt": "2025-02-01T12:00:00",
    "updatedAt": "2025-02-01T12:00:00"
  }
]
```

### `GET /api/companies/{id}`

Returns a single company by ID.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Company ID  |

**Response:** `200 OK` — ExternalCompany object (see above).

### `POST /api/companies`

Creates a new company. The request body is validated.

**Request Body:**
```json
{
  "companyName": "Acme Corp",
  "address": "456 Business Ave",
  "contactPerson": "Bob Jones",
  "phoneNumber": "+1-555-0200"
}
```

| Field           | Type   | Required | Description           |
|-----------------|--------|----------|-----------------------|
| `companyName`   | String | Yes      | Company name          |
| `address`       | String | No       | Company address       |
| `contactPerson` | String | No       | Primary contact name  |
| `phoneNumber`   | String | No       | Contact phone number  |

**Response:** `200 OK` — The created ExternalCompany object.

### `PUT /api/companies/{id}`

Updates an existing company. The path `id` overrides any ID in the body.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Company ID  |

**Request Body:** Same as POST.

**Response:** `200 OK` — The updated ExternalCompany object.

### `DELETE /api/companies/{id}`

Deletes a company by ID.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Company ID  |

**Response:** `200 OK`
```json
{
  "message": "Company deleted successfully"
}
```

---

## Accounts

### `GET /api/accounts`

Returns all accounts enriched with customer role assignments.

**Response:** `200 OK`
```json
[
  {
    "accountId": 1,
    "accountNumber": "ACC-001",
    "accountType": "PERSONAL",
    "balance": 5000.00,
    "currency": "USD",
    "status": "ACTIVE",
    "interestRate": 1.5,
    "openedDate": "2024-06-01",
    "lastActivityDate": "2025-03-15",
    "roles": "Jane Doe (OWNER), Bob Smith (ADMIN)"
  }
]
```

The `roles` field is a comma-separated string showing each linked customer name and their role on the account.

### `GET /api/accounts/{id}`

Returns a single account by ID (without the enriched roles string).

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Account ID  |

**Response:** `200 OK` — Account object.

### `POST /api/accounts`

Creates a new account. The request body is validated.

**Request Body:**
```json
{
  "accountNumber": "ACC-001",
  "accountType": "PERSONAL",
  "balance": 5000.00,
  "currency": "USD",
  "status": "ACTIVE",
  "interestRate": 1.5,
  "openedDate": "2024-06-01",
  "lastActivityDate": "2025-03-15"
}
```

| Field              | Type       | Required | Description                                      |
|--------------------|------------|----------|--------------------------------------------------|
| `accountNumber`    | String     | Yes      | Unique account number                            |
| `accountType`      | String     | Yes      | One of: `SAVINGS`, `CORPORATE`, `PERSONAL`       |
| `balance`          | BigDecimal | Yes      | Current balance                                  |
| `currency`         | String     | Yes      | Currency code (e.g. `USD`, `EUR`)                |
| `status`           | String     | No       | Account status (e.g. `ACTIVE`, `CLOSED`)         |
| `interestRate`     | BigDecimal | No       | Annual interest rate                             |
| `openedDate`       | String     | No       | Date the account was opened (`YYYY-MM-DD`)       |
| `lastActivityDate` | String     | No       | Date of last activity (`YYYY-MM-DD`)             |

**Response:** `200 OK` — The created Account object.

### `PUT /api/accounts/{id}`

Updates an existing account. The path `id` overrides any ID in the body.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Account ID  |

**Request Body:** Same as POST.

**Response:** `200 OK` — The updated Account object.

### `DELETE /api/accounts/{id}`

Deletes an account by ID.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Account ID  |

**Response:** `200 OK`
```json
{
  "message": "Account deleted successfully"
}
```

---

## Customer-Account Links

Links connect Customers to Accounts with a specific role. A customer can be linked to multiple accounts and vice versa, but each (customer, account) pair is unique.

### `GET /api/customer-accounts`

Returns all customer-account links.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "customerId": 1,
    "customerName": "Jane Doe",
    "accountId": 1,
    "accountNumber": "ACC-001",
    "role": "OWNER",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
]
```

### `POST /api/customer-accounts`

Creates or upserts a customer-account link. If the link already exists, the role is updated.

**Request Body:**
```json
{
  "accountId": 1,
  "customerId": 1,
  "role": "OWNER"
}
```

| Field        | Type   | Required | Description                            |
|--------------|--------|----------|----------------------------------------|
| `accountId`  | Long   | Yes      | Target account ID                      |
| `customerId` | Long   | Yes      | Target customer ID                     |
| `role`       | String | Yes      | One of: `OWNER`, `ADMIN`, `NAMED`      |

**Response:** `200 OK`
```json
{
  "id": 1,
  "accountId": 1,
  "accountNumber": "ACC-001",
  "customerId": 1,
  "customerName": "Jane Doe",
  "role": "OWNER",
  "action": "CREATED"
}
```

The `action` field will be either `"CREATED"` (new link) or `"UPDATED"` (existing link's role was changed).

### `PUT /api/customer-accounts`

Updates an existing customer-account link's role. Behaves identically to POST (upsert).

**Request Body:** Same as POST.

**Response:** Same as POST.

### `DELETE /api/customer-accounts/{accountId}/{customerId}`

Removes a customer-account link.

| Parameter    | Type | Location | Description |
|--------------|------|----------|-------------|
| `accountId`  | Long | Path     | Account ID  |
| `customerId` | Long | Path     | Customer ID |

**Response:** `200 OK`
```json
{
  "message": "Customer-account link removed successfully"
}
```

**Error:** `400 Bad Request` if the link does not exist.

---

## Zeebe Connection & Worker Status

### `GET /api/connection-status`

Checks whether the application is connected to the Camunda 8 Zeebe engine.

**Response:** `200 OK`
```json
{
  "connected": true
}
```

When disconnected:
```json
{
  "connected": false,
  "error": "Connection refused: localhost:26500"
}
```

### `GET /api/worker-status`

Returns the polling status of all registered Zeebe job workers.

**Response:** `200 OK`
```json
{
  "match-customer-with-dri": "RUNNING",
  "query-for-company": "RUNNING",
  "search-employee": "RUNNING",
  "search-account": "RUNNING",
  "manage-account-record": "RUNNING",
  "manage-customer-record": "RUNNING",
  "manage-employee-record": "RUNNING",
  "manage-company-record": "RUNNING",
  "manage-customer-account-link": "RUNNING"
}
```

Each worker will report either `"RUNNING"` or `"STOPPED"` depending on whether polling is active.

---

## Job History & Metrics

### `GET /api/job-history`

Returns recent job execution history, newest first.

| Parameter | Type | Location | Default | Description                                |
|-----------|------|----------|---------|--------------------------------------------|
| `limit`   | int  | Query    | `200`   | Max records to return (clamped to 1–1000)  |

**Response:** `200 OK`
```json
[
  {
    "id": 42,
    "jobType": "search-employee",
    "jobKey": "2251799813685312",
    "status": "COMPLETED",
    "executionTime": "2025-03-15 14:23:05",
    "duration": "45ms",
    "inputParameters": { "employeeName": "John" },
    "results": { "employeeId": 10, "fullName": "John Smith" },
    "errorMessage": null,
    "workerName": "Employee Search Service",
    "processDefinition": null,
    "rawInputVariables": "{\"employeeName\":\"John\"}",
    "rawOutputVariables": "{\"employeeId\":10}"
  }
]
```

**Worker display names:**

| Job Type                      | Display Name                    |
|-------------------------------|---------------------------------|
| `match-customer-with-dri`     | Customer-DRI Matcher            |
| `query-for-company`           | Company Query Service           |
| `search-employee`             | Employee Search Service         |
| `search-account`              | Account Search Service          |
| `manage-account-record`       | Account CRUD Service            |
| `manage-customer-record`      | Customer CRUD Service           |
| `manage-employee-record`      | Employee CRUD Service           |
| `manage-company-record`       | Company CRUD Service            |
| `manage-customer-account-link`| Customer-Account Link Service   |

### `GET /api/job-metrics`

Returns aggregate job processing statistics.

**Response:** `200 OK`
```json
{
  "totalJobs": 1523,
  "jobsToday": 42,
  "jobsByType": {
    "search-employee": 320,
    "search-account": 280,
    "manage-customer-record": 150,
    "query-for-company": 200
  }
}
```

---

## Engine Events

### `POST /api/engine/events/publish`

Publishes a MESSAGE or SIGNAL event to the Camunda 8 Zeebe engine.

**Request Body (MESSAGE):**
```json
{
  "eventType": "MESSAGE",
  "name": "payment-received",
  "correlationKey": "order-12345",
  "messageId": "msg-001",
  "timeToLiveMs": 60000,
  "variables": {
    "amount": 99.99,
    "currency": "USD"
  }
}
```

| Field            | Type   | Required           | Description                                       |
|------------------|--------|--------------------|---------------------------------------------------|
| `eventType`      | String | Yes                | `MESSAGE` or `SIGNAL`                             |
| `name`           | String | Yes                | Event name (message or signal name in the BPMN)   |
| `correlationKey` | String | Yes (MESSAGE only) | Correlation key to match the waiting process      |
| `messageId`      | String | No                 | Optional unique message ID for deduplication       |
| `timeToLiveMs`   | Long   | No                 | Time-to-live in milliseconds (MESSAGE only)        |
| `variables`      | Object | No                 | Key-value payload to pass to the process           |

**Request Body (SIGNAL):**
```json
{
  "eventType": "SIGNAL",
  "name": "global-alert",
  "variables": {
    "severity": "HIGH"
  }
}
```

**Response:** `200 OK`
```json
{
  "accepted": true,
  "eventType": "MESSAGE",
  "name": "payment-received",
  "correlationKey": "order-12345",
  "messageId": "msg-001",
  "timeToLiveMs": 60000,
  "timestamp": "2025-03-15T14:30:00+00:00",
  "details": { }
}
```

**Error:** `500 Internal Server Error` if Zeebe connection is not available.

---

## Engine Event Presets

Presets allow saving reusable event configurations for quick re-dispatch.

### `GET /api/engine/event-presets`

Lists all saved presets for a given event type.

| Parameter   | Type   | Location | Required | Description              |
|-------------|--------|----------|----------|--------------------------|
| `eventType` | String | Query    | Yes      | `MESSAGE` or `SIGNAL`    |

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Test Payment Message",
    "eventType": "MESSAGE",
    "eventName": "payment-received",
    "correlationKey": "order-12345",
    "messageId": null,
    "timeToLiveMs": 60000,
    "payloadText": "{\"amount\": 99.99}",
    "createdAt": "2025-03-10T09:00:00",
    "updatedAt": "2025-03-10T09:00:00"
  }
]
```

### `POST /api/engine/event-presets`

Saves a new event preset.

**Request Body:**
```json
{
  "name": "Test Payment Message",
  "eventType": "MESSAGE",
  "eventName": "payment-received",
  "correlationKey": "order-12345",
  "messageId": null,
  "timeToLiveMs": 60000,
  "payloadJson": "{\"amount\": 99.99}"
}
```

| Field            | Type   | Required           | Description                                              |
|------------------|--------|--------------------|----------------------------------------------------------|
| `name`           | String | Yes                | Unique preset name (unique per event type)               |
| `eventType`      | String | Yes                | `MESSAGE` or `SIGNAL`                                    |
| `eventName`      | String | Yes                | The event name in the BPMN model                         |
| `correlationKey` | String | Yes (MESSAGE only) | Correlation key for message events                       |
| `messageId`      | String | No                 | Optional message ID                                      |
| `timeToLiveMs`   | Long   | No                 | Time-to-live in ms                                       |
| `payloadJson`    | String | Yes                | JSON payload string. Alternatively use `payloadText`     |

**Response:** `200 OK` — The saved preset object (same shape as GET response).

### `DELETE /api/engine/event-presets/{id}`

Deletes a preset by ID.

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `id`      | Long | Path     | Preset ID   |

**Response:** `200 OK`
```json
{
  "deleted": true,
  "id": 1
}
```

---

## Engine Event Dispatches

Audit log of events that have been dispatched to Zeebe.

### `GET /api/engine/event-dispatches`

Lists recent event dispatch records.

| Parameter   | Type   | Location | Required | Default | Description                          |
|-------------|--------|----------|----------|---------|--------------------------------------|
| `eventType` | String | Query    | No       | —       | Filter by `MESSAGE` or `SIGNAL`      |
| `limit`     | int    | Query    | No       | `50`    | Max records to return                |

**Response:** `200 OK`
```json
[
  {
    "id": 10,
    "eventType": "MESSAGE",
    "name": "payment-received",
    "correlationKey": "order-12345",
    "success": true,
    "detail": "accepted",
    "responseJson": "{}",
    "payloadJson": "{\"amount\": 99.99}",
    "time": "2025-03-15T14:30:00"
  }
]
```

When `success` is `false`, the `detail` field contains the error message.

### `DELETE /api/engine/event-dispatches`

Clears event dispatch audit records.

| Parameter   | Type   | Location | Required | Description                                     |
|-------------|--------|----------|----------|-------------------------------------------------|
| `eventType` | String | Query    | No       | If provided, only clears records of that type   |

**Response:** `200 OK`
```json
{
  "deleted": 42
}
```

---

## Data Models Reference

### Enums

| Enum          | Values                              |
|---------------|-------------------------------------|
| `AccountType` | `SAVINGS`, `CORPORATE`, `PERSONAL`  |
| `AccountRole` | `OWNER`, `ADMIN`, `NAMED`           |
| `TrustLevel`  | `L1`, `L2`, `L3`                    |
| `EngineEventType` | `MESSAGE`, `SIGNAL`            |

### Common Audit Fields

All entity objects include auto-managed audit timestamps:

| Field       | Type            | Description                        |
|-------------|-----------------|------------------------------------|
| `createdAt` | LocalDateTime   | Set automatically on creation      |
| `updatedAt` | LocalDateTime   | Updated automatically on changes   |

### Actuator Endpoints

Spring Boot Actuator endpoints are also exposed:

| Endpoint             | Description          |
|----------------------|----------------------|
| `GET /actuator/health`  | Application health |
| `GET /actuator/info`    | Application info   |
| `GET /actuator/metrics` | Metrics data       |
