Scaffold a brand-new data entity end-to-end across the whole stack: JPA model (+ enums + not-found exception), repository, service, a single `manage-<entity>` CRUD+QUERY Zeebe worker, worker registration, REST endpoints, seed data, the dashboard UI (tab, table, modal, JS wiring), and the connector element template. The user must specify the entity name and its fields — ask if not provided.

This is the bigger sibling of `/add-entity-field`. Use that one instead when adding a single field to an entity that already exists.

## Canonical reference — copy from InsurancePolicy

`InsurancePolicy` is the newest, cleanest entity and the template to follow. It folds QUERY into the CRUD worker (no separate search worker), which is the pattern this skill produces. **Read these files first and mirror their structure for the new entity** — do not invent new patterns:

- `src/main/java/com/example/camunda/model/InsurancePolicy.java` + `model/PolicyType.java` (enum example)
- `src/main/java/com/example/camunda/exception/InsurancePolicyNotFoundException.java`
- `src/main/java/com/example/camunda/repository/InsurancePolicyRepository.java`
- `src/main/java/com/example/camunda/service/InsurancePolicyService.java`
- `src/main/java/com/example/camunda/worker/InsurancePolicyCrudWorker.java`
- `element-templates/manage-insurance-policy.json`

The registration points in `ZeebeJobPollingService`, `DataController`, `DataInitializer`, and `dashboard.html` all already have an InsurancePolicy entry — grep for `insurance`/`policy`/`policies` in each to find exactly where the parallel new lines go.

---

## What to collect before starting

- **Entity name** — PascalCase singular (e.g. `Package`). Drives class names.
- **Plural / collection name** — lowercase (e.g. `packages`). Drives table name, API path, dashboard keys.
- **ID field** — `<entity>Id` Long (e.g. `packageId`). Client may supply it; auto-generated via `findMax…Id() + 1` when omitted, exactly like `policyId`.
- **Optional human-readable code field** — like `policyNumber`/`accountNumber` (e.g. `trackingNumber`). Auto-formatted in the service if blank. Skip if not wanted.
- **Fields** — for each: name (camelCase), Java type (`String`, `Long`, `Integer`, `BigDecimal`, `LocalDate`, `Boolean`, or an enum), and **required on CREATE?**
- **Enums** — any field whose value is a fixed set; collect the allowed values.
- **Dashboard chrome** — a Bootstrap colour (`primary`/`success`/`info`/`warning`/`danger`/`secondary`) and a Font Awesome icon class (e.g. `fa-box`) for the tab, count card, and worker card.

Naming derived from the example `Package` / `packages`:

| Thing | Value |
|---|---|
| Table | `@Table(name = "packages")` |
| Repository | `PackageRepository`, query `findMaxPackageId()` |
| Service | `PackageService`, field `packageRepository` |
| Exception | `PackageNotFoundException extends BusinessException` |
| Worker | `PackageCrudWorker` |
| **Zeebe job type** | `manage-package` |
| Result map keys | `packageCrudResult`, `packageCount`, `operationStatus` |
| Connector template | `element-templates/manage-package.json`, id `com.example.camunda.manage-package` |
| REST base | `/api/packages` |
| Dashboard entity key | `packages`; tableId `packages-table`; idField `packageId` |

---

# Backend

## Step 1 — Enums (if any)

File(s): `src/main/java/com/example/camunda/model/<EnumName>.java`

Plain enums, one per file (see `model/PolicyType.java`):
```java
package com.example.camunda.model;

public enum PackageStatus {
    CREATED, IN_TRANSIT, DELIVERED, RETURNED
}
```

## Step 2 — Model entity

File: `src/main/java/com/example/camunda/model/<Entity>.java`

Mirror `InsurancePolicy.java`:
- `@Entity`, `@Table(name = "<plural>")`, `@EntityListeners(AuditingEntityListener.class)`
- `@Id` on the `<entity>Id` Long (with `@Positive`); **no `@GeneratedValue`** — the service assigns IDs
- Optional code field: `@Column(nullable = false, unique = true) private String <codeField>;`
- Enum fields: `@Enumerated(EnumType.STRING)` + `@Column(nullable = false)` + `@NotNull(...)` when required
- `BigDecimal` money: `@Column(precision = 12, scale = 2)`
- `createdAt`/`updatedAt`: `@CreatedDate`/`@LastModifiedDate` (copy verbatim)
- Getter + setter for every field

Nullable columns need no annotation — Hibernate `ddl-auto=update` evolves the H2 schema automatically; there are no migration files.

## Step 3 — Not-found exception

File: `src/main/java/com/example/camunda/exception/<Entity>NotFoundException.java`

Copy `InsurancePolicyNotFoundException.java` verbatim, renaming the class. It extends `BusinessException`, which `GlobalExceptionHandler` already maps to a 404 — nothing to register.

## Step 4 — Repository

File: `src/main/java/com/example/camunda/repository/<Entity>Repository.java`

```java
public interface PackageRepository extends JpaRepository<Package, Long>, JpaSpecificationExecutor<Package> {
    Optional<Package> findByTrackingNumber(String trackingNumber);   // if a code field exists
    // a finder per common filter field, e.g. List<Package> findByStatus(PackageStatus status);

    @Query("SELECT COALESCE(MAX(p.packageId), 0) FROM Package p")
    Long findMaxPackageId();
}
```
`JpaSpecificationExecutor` is required for the QUERY operation's dynamic filtering.

## Step 5 — Service

File: `src/main/java/com/example/camunda/service/<Entity>Service.java`

Mirror `InsurancePolicyService`: `@Service`, `@Transactional(readOnly = true)`, constructor-injected repository. Provide `getAll…`, `get…ById` (throws the not-found exception), per-filter getters, a `search…(...)` built from `Specification.where(null).and(...)` for each non-null criterion, and a `@Transactional save…` that assigns the ID from `findMax…Id()` when null and formats the code field when blank, plus a `@Transactional delete…` that checks existence first.

## Step 6 — CRUD + QUERY worker

File: `src/main/java/com/example/camunda/worker/<Entity>CrudWorker.java`

Copy `InsurancePolicyCrudWorker.java` and adapt. It is `@Component`, constructor-injects the service, and exposes `handleJob(ActivatedJob)`. Keep every piece:

- `handleJob` → `normalizeOperation` → switch over `CREATE`/`UPDATE`/`DELETE`/`QUERY`, wrapped in try/catch returning `buildErrorResult`.
- **`handleCreate`** — `extract…` each variable, run `validateCreateInput(...)` (return `buildValidationError` listing required fields), reject if the ID already exists, build the entity, save, `buildSuccessResult`.
- **`handleUpdate`** — require the ID (else validation error), load (catch not-found → `buildNotFoundResult`), patch-style: `if (x != null) entity.setX(x)`, save.
- **`handleDelete`** — require ID, check exists, delete, return the delete result map.
- **`handleQuery`** — if ID present, single-record shortcut; else `service.search(...)`; map each row through `build<Entity>Data(...)`; return list under `<entity>CrudResult.<plural>` plus `count`.
- **Result builders** — `buildSuccessResult`, `build<Entity>Data` (the map of every field, enums via `.name()`, dates via `.toString()`), `buildNotFoundResult`, `buildValidationError`, `buildErrorResult`. Each nests a `<entity>CrudResult` map and also sets top-level `operationStatus`, `operation`, and the id/code shortcuts.
- **Extract helpers** — keep `extractString`, `extractLong`, `extractInteger`, `extractBigDecimal`, `extractLocalDate`, and one `extract<Enum>` per enum. Add `extractBoolean` only if a Boolean field exists (see `/add-entity-field` for its body).

## Step 7 — Register the worker (poller)

File: `src/main/java/com/example/camunda/ZeebeJobPollingService.java`

Four edits, paralleling `insurancePolicyCrudWorker`:
1. `import com.example.camunda.worker.<Entity>CrudWorker;`
2. Add a `private final <Entity>CrudWorker <field>;`
3. Add it as a constructor parameter **and** assign `this.<field> = <field>;`
4. Add a polling method:
```java
@Scheduled(fixedDelay = 1000)
@Async
public void pollPackageCrudJobs() {
    if (isRunning.get()) {
        pollJobs("manage-package", packageCrudWorker::handleJob);
    }
}
```

## Step 8 — REST endpoints

File: `src/main/java/com/example/camunda/controller/DataController.java`

Mirror the InsurancePolicy block:
1. Import the model + service; add a `private final <Entity>Service` field; add it to the constructor params and assignments.
2. Add `GET /<plural>`, `GET /<plural>/{id}`, `POST /<plural>`, `PUT /<plural>/{id}` (sets id then saves), `DELETE /<plural>/{id}` (returns `{"message": "..."}`).
3. In `getWorkerStatus()`, add `status.put("manage-package", workerStatus);`.

---

# Data & UI

## Step 9 — Seed data

File: `src/main/java/com/example/camunda/DataInitializer.java`

- Add the repository to the `initData(...)` `CommandLineRunner` parameter list.
- If `resetOnStartup` deletion order matters (FK dependencies), add `<entity>Repo.deleteAll();` in the right place inside the `if (resetOnStartup)` block.
- Add a `seed<Entities>(...)` guarded by `if (<entity>Repo.count() == 0)`, following the existing seed methods: a few realistic hardcoded records plus a generated loop (reuse the `GLOBAL_*` name/location arrays and `java.util.Random` for variety).

## Step 10 — Dashboard

File: `src/main/resources/templates/dashboard.html`

This is the most error-prone step — there are **eleven** separate spots, several of them JS lookup maps where a missing entry silently breaks the table. Grep for `policies` / `policies-table` / `Policy` to locate each parallel insertion point.

**HTML:**
1. **Overview count card** (~line 556) — copy the Insurance Policies `col-md-3` card; new id `<plural>-count`, your icon + colour.
2. **Data-source nav pill** (~line 597) — copy the `policies-data-tab` `<li>`; `data-bs-target="#<plural>-data"`.
3. **Tab pane + table** (~line 699) — copy the `policies-data` pane; card header with an `onclick="showAdd<Entity>Modal()"` button and `<table … id="<plural>-table">`.
4. **Modal** (~line 919) — copy the entire Insurance Policy modal: a `<form id="<entity>Form">` with an input per field (`id="<entity>...Input"`), a hidden/disabled-on-edit ID input, an error div `<entity>ModalError`, and a footer Save button `id="save<Entity>Btn" onclick="save<Entity>()"`. Use `<select>` for enums.

**JS — registration maps (all near line 2157):**
5. `entityTableMap` — add `<plural>: { tableId: '<plural>-table', idField: '<entity>Id', apiPath: '<plural>', displayColumns: [...] }`. Set `apiPath` only if it differs from the key; list `displayColumns` to control table columns (otherwise all keys show).
6. `tableDataCache` — add `'<plural>-table': []`.
7. `tableViewState` — add `'<plural>-table': { filter: '', sortKey: '', sortDir: 'asc' }`.
8. `tabPaneToTableMap` — add `'<plural>-data': '<plural>-table'`.

**JS — behaviour:**
9. `renderTable` count update (~line 2404) — add an `else if (tableId === '<plural>-table') { document.getElementById('<plural>-count').textContent = processedData.length; }`.
10. `canEdit` list (~line 2368) — add `|| entityType === '<plural>'` so the row Edit button appears.
11. `refreshAll()` (~line 2480) **and** `refreshDataTables()` (~line 3012) — add `fetchAndRenderTable('/api/<plural>', '<plural>-table');` to both.
12. Edit loader (`editEntity`, ~line 2639) — add an `else if (entityType === '<plural>')` branch that fetches `/api/<plural>/${entityId}` and populates each modal input.
13. `showAdd<Entity>Modal()`, `save<Entity>()`, and any `update…Fields()` show/hide helpers — copy `showAddPolicyModal`/`savePolicy` (~line 3015+). `save…` builds the request body from the inputs, picks POST vs PUT on `currentEditId`, posts to `/api/<plural>`, and calls `applyOptimisticEntitySave('<plural>', data, Boolean(currentEditId))`.
14. Worker card metadata — in the `getWorkerInfo`/worker-card `switch` (~line 1200) add a `case 'manage-package':` returning `{ name, description, icon }`.

---

## Step 11 — Connector template

File: `element-templates/manage-<entity>.json`

Copy `manage-insurance-policy.json` and adapt. Keep: the `$schema`, `appliesTo`/`elementType` `bpmn:ServiceTask`, the hidden `zeebe:taskDefinition:type` set to `manage-<entity>`, the `operation` Dropdown (CREATE/UPDATE/DELETE/QUERY), and the two output properties (`<entity>CrudResult` and `operationStatus`).

Per field, follow the policy template's conventions:
- The **ID** gets four entries (`…Create`/`…Update`/`…Delete`/`…Query`) differing by `condition.equals` and whether `constraints.notEmpty` is set — see `policyIdCreate`…`policyIdQuery`.
- **Editable fields** use `"condition": { "property": "operation", "oneOf": ["CREATE", "UPDATE"] }`.
- **QUERY filters** are separate Text properties in a `query` group with `condition … equals "QUERY"`.
- **Enums** use `"type": "Dropdown"` with `choices`.
- Group related fields with the `groups` array.

Set `"version": 1` for a new template. (When editing an existing template later, bump the version.) Validate the JSON is well-formed before finishing.

---

## Verify & finish

- Compile: `mvn compile` (no Maven wrapper in this repo). Fix any missing-import / missing-`extract*`-helper errors — add helpers *before* wiring them.
- Sanity-check the dashboard by loading it if the app is running (see `/run` or `/verify`); confirm the new tab, table, count card, and Add/Edit modal work.

### Checklist
- [ ] Enum file(s) created
- [ ] Model entity (id without `@GeneratedValue`, getters/setters, audit fields)
- [ ] `<Entity>NotFoundException`
- [ ] Repository (`JpaSpecificationExecutor` + `findMax…Id`)
- [ ] Service (search via Specification, save assigns id, delete checks existence)
- [ ] CRUD worker (create/update/delete/query, result builders, extract helpers)
- [ ] Worker registered in `ZeebeJobPollingService` (import, field, ctor param + assign, poll method with job type `manage-<entity>`)
- [ ] `DataController` (service injected, 5 endpoints, `worker-status` entry)
- [ ] Seed data (repo param, optional reset-order, `seed…` method)
- [ ] Dashboard: count card, nav pill, tab pane + table, modal
- [ ] Dashboard JS maps: `entityTableMap`, `tableDataCache`, `tableViewState`, `tabPaneToTableMap`
- [ ] Dashboard JS: count update, `canEdit`, `refreshAll` + `refreshDataTables`, edit loader, show/save handlers, worker-card `getWorkerInfo` case
- [ ] Connector template `manage-<entity>.json` (version 1, valid JSON)
- [ ] `mvnw compile` passes
