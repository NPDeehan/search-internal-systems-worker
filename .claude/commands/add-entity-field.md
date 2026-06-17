Add a new field to an existing data entity in this project. The user must specify the entity name and field details. Ask if not provided.

## What to collect before starting

- **Entity** — which entity to update (Customer, Employee, ExternalCompany, Account, InsurancePolicy)
- **Field name** — camelCase Java name (e.g. `phoneNumber`)
- **Field type** — Java type (String, Long, BigDecimal, LocalDate, Boolean, or an enum)
- **Required on CREATE?** — yes/no
- **Exposed in the connector template?** — yes/no (ask; not all entities have a manage-* worker)

---

## Step 1 — Entity class

File: `src/main/java/com/example/camunda/model/<Entity>.java`

Add the field, getter, and setter following the existing pattern:

```java
@Column(name = "field_name")   // snake_case of the camelCase field
private FieldType fieldName;

public FieldType getFieldName() { return fieldName; }
public void setFieldName(FieldType fieldName) { this.fieldName = fieldName; }
```

Rules:
- Enums get `@Enumerated(EnumType.STRING)` instead of `@Column`
- Nullable fields need no extra annotation — H2 schema is auto-updated by Hibernate (`ddl-auto=update`), no migration files exist
- Place the field near others of the same type (strings with strings, dates with dates)

---

## Step 2 — DataInitializer seed data

File: `src/main/java/com/example/camunda/DataInitializer.java`

Two places to update:

1. **Hardcoded records** — find the hardcoded `new Entity()` blocks for this entity and set a realistic value on each one.
2. **Generated records** — find the loop that creates the 100 generated records and set a value there too (use randomness if appropriate to make the data varied).

---

## Step 3 — Zeebe workers

Most entities have **two** workers, not one. Always check for both:

| Entity | CRUD worker | Query/search worker |
|---|---|---|
| Customer | `CustomerCrudWorker` | `MatchCustomerWithDriWorker` |
| Employee | `EmployeeCrudWorker` | `EmployeeSearchWorker` |
| ExternalCompany | `CompanyCrudWorker` | `QueryForCompanyWorker` |
| Account | `AccountCrudWorker` | `AccountSearchWorker` |
| InsurancePolicy | `InsurancePolicyCrudWorker` | *(none)* |

### Step 3a — CRUD worker

File: `src/main/java/com/example/camunda/worker/<Entity>CrudWorker.java`

**First: check whether an `extract*` helper exists for the field's type.** Each worker has private helpers at the bottom (`extractString`, `extractLong`, etc.). If the type is new (e.g. `Boolean`, `BigDecimal`, `LocalDate`), add the helper to the worker class *before* wiring it up in `handleCreate`/`handleUpdate` — otherwise the compiler will flag missing method errors mid-edit. Pattern:

```java
private Boolean extractBoolean(Object value) {
    if (value == null) return null;
    if (value instanceof Boolean b) return b;
    String str = value.toString().trim();
    if (str.equalsIgnoreCase("true")) return true;
    if (str.equalsIgnoreCase("false")) return false;
    return null;
}
```

Then update the three methods in the CRUD worker:

### handleCreate
```java
FieldType fieldName = extractFieldType(variables.get("fieldName"));
// add to validation if required:
if (fieldName == null) return buildValidationError("fieldName is required for CREATE", "CREATE");
// set on entity (for optional booleans default to false rather than null):
entity.setFieldName(fieldName != null ? fieldName : false);
```

### handleUpdate
```java
FieldType fieldName = extractFieldType(variables.get("fieldName"));
// patch-style — only update if provided:
if (fieldName != null) entity.setFieldName(fieldName);
```

### buildSuccessResult
Add the field to the `entityData` map so it's returned in the Zeebe job result:
```java
entityData.put("fieldName", entity.getFieldName());
```

### Step 3b — Query/search worker

File: `src/main/java/com/example/camunda/worker/<Entity>SearchWorker.java` (or `QueryFor<Entity>Worker.java`)

These workers build a `entityData` map manually per result row inside a `.stream().map(entity -> { ... })` block. The new field must be added there too, or it will be silently absent from query results even though the CRUD worker returns it correctly.

```java
companyData.put("fieldName", entity.getFieldName());
```

The query worker's connector template (`query-for-<entity>.json`) does **not** need updating — it captures the full result object, so the new field appears inside it automatically once the worker returns it.

---

## Step 4 — Dashboard HTML

File: `src/main/resources/templates/dashboard.html`

**Table columns are auto-generated** from the API response keys via `Object.keys()` in `renderTable`, so no manual header/row edits are needed — the new field will appear automatically once the API returns it. The exception is entities that have a `displayColumns` list in `entityTableMap` (currently only `policies`); for those, add the field name to that array.

**Boolean fields** render as colour-coded badges (`bg-danger Yes` / `bg-secondary No`) automatically — the `renderTable` function already handles `typeof val === 'boolean'`. No extra work needed.

**Modal form** — each entity has an Add/Edit modal. Three places to update:

1. **Form field** — add an `<input>`, `<select>`, or `<input type="checkbox">` inside the `<form>` block for the entity's modal.
2. **Edit loader** — find where the modal is populated when editing (search for the entity's modal ID + `.value =`). Add a line to set the new input from the fetched entity.
3. **Save handler** — find the `saveEntity()` function (e.g. `saveCompany()`). Add the new field to the request body object being sent to the API.

---

## Step 5 — Connector template (if applicable)

File: `element-templates/manage-<entity>-record.json`

Each field that goes through the Zeebe worker needs **two property entries** — one per operation (CREATE and UPDATE). They share the same `binding.name` but have different `id`, `description`, `constraints`, and `condition` values.

Pattern for a String field:
```json
{
  "id": "fieldNameCreate",
  "label": "Field Label",
  "description": "Required for CREATE.",
  "type": "Text",
  "group": "input",
  "feel": "optional",
  "binding": { "type": "zeebe:input", "name": "fieldName" },
  "constraints": { "notEmpty": true },
  "condition": { "property": "operation", "equals": "CREATE", "type": "simple" }
},
{
  "id": "fieldNameUpdate",
  "label": "Field Label",
  "description": "Optional for UPDATE.",
  "type": "Text",
  "group": "input",
  "feel": "optional",
  "binding": { "type": "zeebe:input", "name": "fieldName" },
  "condition": { "property": "operation", "equals": "UPDATE", "type": "simple" }
}
```

Rules:
- Place the two entries together, near other fields of the same kind, before the output properties
- For **enum fields** use `"type": "Dropdown"` with `"choices"` and add a `"pattern"` constraint `"^(VALUE1|VALUE2|=.+)$"` to allow FEEL expressions
- For **boolean fields** use `"type": "Dropdown"` with `"choices": [{"name":"false","value":"false"},{"name":"true","value":"true"}]`. For the UPDATE entry, add a third `{"name":"(no change)","value":""}` choice as the default so omitting the field leaves the existing value unchanged
- After editing, **bump `"version"` by 1** at the top of the file
- Validate the JSON is well-formed before finishing

---

## Checklist before finishing

- [ ] Entity field + getter/setter added
- [ ] Seed data updated (hardcoded records + generated loop; helper method signature updated if it takes explicit args)
- [ ] CRUD worker: new `extract*` helper added first if the type is new
- [ ] CRUD worker: handleCreate extracts + validates + sets field
- [ ] CRUD worker: handleUpdate extracts + conditionally sets field
- [ ] CRUD worker: buildSuccessResult includes field in response map
- [ ] Query/search worker: field added to the per-row `entityData` map (if a query worker exists for this entity)
- [ ] Dashboard modal form field added
- [ ] Dashboard edit loader populates the new form field
- [ ] Dashboard save handler includes the field in the request body
- [ ] (policies only) `displayColumns` array updated in `entityTableMap`
- [ ] Connector template updated with CREATE + UPDATE properties and version bumped
- [ ] Connector template JSON is valid
