# Add Entity Variable Playbook

Use this checklist every time a new variable is added to an existing data entity.

Scope covered:
- Database/schema behavior
- Seed/test data generation
- Service/repository search logic
- Worker input/output contracts
- ID ownership strategy for CREATE operations
- Dashboard frontend CRUD wiring
- Camunda element template updates
- Connector/docs registration consistency checks
- Test updates and verification

---

## 0) Define the change contract (mandatory)

Before coding, capture these decisions for the new variable:

- Entity: `Customer` / `Employee` / `ExternalCompany` / `Account`
- Field name (Java + JSON): e.g. `industry`
- Data type: `String`, `Long`, `BigDecimal`, `LocalDate`, etc.
- If value set is closed (e.g. `L1/L2/L3`), prefer an enum + `@Enumerated(EnumType.STRING)`
- Required or optional (`nullable`)
- Validation rules (`@NotBlank`, ranges, enums)
- CREATE ID strategy: caller-supplied vs service-generated (must be explicit)
- Is it searchable input for a worker?
- Is it part of worker output to BPMN?
- Must it be editable on dashboard CRUD forms?
- Must it be shown in dashboard tables?

If any answer is unknown, stop and decide first.

---

## 1) Update domain model and DB behavior

### 1.1 Entity class
Update the target model in `src/main/java/com/example/camunda/model/`:
- Add field
- Add JPA annotation (`@Column(...)`, `@Enumerated`, etc.)
- Add bean validation annotations if required
- Add getter/setter

Examples:
- `src/main/java/com/example/camunda/model/Employee.java`
- `src/main/java/com/example/camunda/model/Customer.java`
- `src/main/java/com/example/camunda/model/ExternalCompany.java`
- `src/main/java/com/example/camunda/model/Account.java`

### 1.2 DB migration strategy for this repo
This repo uses `spring.jpa.hibernate.ddl-auto=update` with persistent file H2 (`data/camunda-worker-db.mv.db`).

Use one of these strategies:
- Optional field: add as nullable and proceed.
- Required field on existing table: either
  - add temporarily nullable + backfill + later enforce not-null, or
   - add a one-time startup backfill hook for existing rows,
  - reset local H2 files after code changes (acceptable for PoC/local dev).

When using startup backfill, keep it idempotent and only update rows where the new field is null.

Reset option available in workspace task: `Reset H2 DB files`.

### 1.3 ID strategy for CREATE flows (mandatory when touching CRUD)

Decide and enforce one strategy consistently:

- **Service-generated IDs (preferred for PoC UX)**
   - Entity ID must not be `@NotNull`
   - Service `save*` method assigns ID when null (`MAX(id)+1` currently used in this repo)
   - Worker CREATE validation must not require ID
   - Element template CREATE ID field must be optional (`notEmpty: false`) and description must mention auto-generation
   - UPDATE/DELETE must still require ID

- **Caller-supplied IDs**
   - Keep ID required for CREATE in worker/template/API contracts
   - Keep duplicate-ID validation in CREATE path

---

## 2) Update seed/test data generation

Update both seeders so local data and tests remain representative:

- `src/main/java/com/example/camunda/DataInitializer.java`
  - update seed methods
  - update helper factory methods (`createEmployee`, `createCustomer`, etc.)
- `src/main/java/com/example/camunda/service/DataSeedingService.java`
  - update generated sample objects

Rule: every newly required field must be populated in both seed paths.

---

## 3) Update repository + service query logic (if searchable)

If new variable is part of search/filter logic:

- Update repository method signatures and/or JPQL
  - `src/main/java/com/example/camunda/repository/EmployeeRepository.java`
  - `src/main/java/com/example/camunda/repository/CustomerRepository.java`
  - `src/main/java/com/example/camunda/repository/ExternalCompanyRepository.java`
  - `src/main/java/com/example/camunda/repository/AccountRepository.java`
- Update corresponding service methods and validation guards
  - `src/main/java/com/example/camunda/service/EmployeeService.java`
  - `src/main/java/com/example/camunda/service/CustomerService.java`
  - `src/main/java/com/example/camunda/service/CompanyService.java`
  - `src/main/java/com/example/camunda/service/AccountService.java`

Keep existing behavior stable for old parameters unless change is explicitly requested.

---

## 4) Update worker contracts

Target workers are in `src/main/java/com/example/camunda/worker/`.

When adding a new variable:

1. Input parsing
   - Extract from job variables map (`extractString`, `extractLong`, etc.)
   - Include in validation rules/messages if required for search

2. Service call
   - Pass variable through to service search method

3. Output mapping
   - Add field to result object(s):
     - consolidated object (`matchingResult`, `employeeSearchResult`, `companySearchResult`, `accountSearchResult`)
     - convenience flat fields (single-record outputs)
   - Ensure success/not-found/error branches all keep a consistent output shape

4. Helper mappers
   - Add new field in map conversion methods (for list outputs)

---

## 5) Update Camunda element template(s)

Templates live in `element-templates/`.

Add/update:
- Input property (`zeebe:input`) if BPMN should provide the new variable
- Output property (`zeebe:output`) if BPMN should receive it
- User-facing label/description/examples
- Constraints (`notEmpty`, FEEL optionality)
- Increment template `version` when contract changes
- Ensure **every user-visible property has a `binding`** (including selector-only fields such as `searchMode`/`lookupMode`) to avoid Modeler warnings like `missing binding for property "<index>"`
- If a field is likely mapped at runtime, avoid Dropdown unless model-time-only selection is intended; prefer Text + regex constraint + FEEL guidance

Files:
- `element-templates/match-customer-with-dri.json`
- `element-templates/search-employee.json`
- `element-templates/query-for-company.json`
- `element-templates/search-account.json`
- `element-templates/manage-customer-record.json`
- `element-templates/manage-employee-record.json`
- `element-templates/manage-company-record.json`
- `element-templates/manage-account-record.json`
- `element-templates/manage-customer-account-link.json`

---

## 6) Update dashboard and REST payload wiring

### 6.1 CRUD forms (if editable)
Update `src/main/resources/templates/dashboard.html`:
- Add input control in the correct modal form
- Prefill in `editEntity(...)`
- Include in payload in `saveCustomer()` / `saveEmployee()` / `saveCompany()`
- Add client-side required handling if needed

### 6.2 Table visibility
Tables are dynamically rendered from object keys.

Usually no table-render code change is needed, but ensure:
- field is present in API payload
- value format is user-friendly (dates/empty strings)

### 6.3 API mapping edge case
Accounts are manually mapped in controller.

If adding account fields, update:
- `src/main/java/com/example/camunda/controller/DataController.java` (`getAccounts` mapper)

For `Customer`, `Employee`, `ExternalCompany`, controller usually returns entity directly.

---

## 7) Update tests

Minimum test updates per variable change:

1. Worker unit tests
   - Add assertions for new input/output variable behavior
   - Example: `src/test/java/com/example/camunda/worker/EmployeeSearchWorkerTest.java`

2. Controller tests (if CRUD/API payload changed)
   - `src/test/java/com/example/camunda/controller/DataControllerTest.java`

3. Repository/service tests (if query logic changed)
   - Add/update tests under `src/test/java/com/example/camunda/service/`
   - Add/update repository integration assertions if entity constraints changed

4. Integration smoke
   - Ensure app starts with schema + seeders with new field

---

## 8) Verification sequence (run every time)

1. Compile/tests
   - Run targeted tests for changed worker/service first
   - Run full `mvn test`

2. Local DB sanity
   - If schema mismatch or non-null change causes issues, run `Reset H2 DB files`, restart app, reseed

3. Dashboard sanity
   - Create/edit entity with new variable
   - Confirm value appears in table and round-trips via API

4. Worker sanity
   - Run BPMN with updated template inputs
   - Confirm output variable exists with expected key/type

5. Template sanity
   - Re-import updated template in Camunda Modeler and verify field visibility

6. Template binding sanity (quick guardrail)
   - Run a missing-binding scan across `element-templates/*.json`
   - Confirm there are no unbound properties

---

## 9) Definition of done

A variable addition is complete only when all are true:

- Entity compiles with correct validation/JPA annotations
- CREATE ID behavior is explicit and consistent across entity/service/worker/template
- Seeder(s) populate valid values
- Search/service/worker logic handles new field correctly
- Element template includes new contract (input/output as needed)
- Dashboard CRUD supports the field (if in scope)
- Tests pass
- Manual checks pass (API + UI + worker output)

---

## 10) If the change introduces a new connector/worker (extension trigger)

If your "variable" change grows into a new job type, also update these (or follow README worker checklist):

- `src/main/java/com/example/camunda/ZeebeJobPollingService.java` (poll registration)
- `src/main/java/com/example/camunda/controller/DataController.java` (`/api/worker-status`)
- `README.md` implemented connectors + worker reference table
- `element-templates/README.md` template inventory

---

## Quick execution template (copy for each change)

Use this at the top of each variable-change ticket/PR:

- Variable: 
- Entity: 
- Type / Nullable: 
- Search input? (Y/N): 
- Worker output? (Y/N): 
- Dashboard editable? (Y/N): 
- Files changed:
  - Model:
  - Seeders:
  - Repository/Service:
  - Worker:
  - Element template:
  - Dashboard:
  - Tests:
- Verification done:
  - [ ] Targeted tests
  - [ ] Full tests
  - [ ] UI round-trip
  - [ ] Worker output check
