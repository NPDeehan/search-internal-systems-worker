# Camunda 8 Element Templates

This directory contains the Camunda Modeler element templates used by this worker application.

## Template Inventory

- `match-customer-with-dri.json`
- `search-employee.json`
- `query-for-company.json`
- `search-account.json`
- `manage-customer-record.json`
- `manage-employee-record.json`
- `manage-company-record.json`
- `manage-account-record.json`
- `manage-customer-account-link.json`

## Worker and Output Mapping

| Template File | Job Type | Primary Output Mapping | Notes |
|---|---|---|---|
| `match-customer-with-dri.json` | `match-customer-with-dri` | `matchingResult` (+ convenience outputs like `matchStatus`, `customerId`, `employeeId`) | Includes `lookupMode` (`CUSTOMER_ID`, `CUSTOMER_NAME`, `ID_OR_NAME`) |
| `search-employee.json` | `search-employee` | `employeeSearchResult` source mapped to target variable `employeeSearchResponse` by default | Includes `searchMode` and fuzzy/exact flags |
| `query-for-company.json` | `query-for-company` | `companySearchResult` | Includes `searchMode` (`COMPANY_NAME`, `INDUSTRY`, `CITY`, `REVENUE`, `FLEXIBLE`) |
| `search-account.json` | `search-account` | `accountSearchResult` | Includes `searchMode` (`ACCOUNT_ID`, `LEGACY_ALIAS`, `ACCOUNT_NUMBER`, `CUSTOMER_ID`, `FLEXIBLE`) |
| `manage-customer-record.json` | `manage-customer-record` | `customerCrudResult`, `operationStatus` | `operation`: `CREATE`, `UPDATE`, `DELETE` |
| `manage-employee-record.json` | `manage-employee-record` | `employeeCrudResult`, `operationStatus` | `operation`: `CREATE`, `UPDATE`, `DELETE` |
| `manage-company-record.json` | `manage-company-record` | `companyCrudResult`, `operationStatus` | `operation`: `CREATE`, `UPDATE`, `DELETE` |
| `manage-account-record.json` | `manage-account-record` | `accountCrudResult`, `operationStatus` | `operation`: `CREATE`, `UPDATE`, `DELETE` |
| `manage-customer-account-link.json` | `manage-customer-account-link` | `customerAccountLinkResult`, `operationStatus` | `operation`: `UPSERT_ROLE`, `REMOVE_LINK` |

## FEEL and Input Rules

- Templates support FEEL expressions for most text fields (for example `=customerId`, `=request.customerId`).
- Many templates use mode selectors (`searchMode`, `lookupMode`, `operation`) with conditional fields.
- Boolean-like fields are modeled as text with constraints in several templates (for example `true`, `false`, or FEEL expression).
- CRUD templates apply operation-specific required fields, so CREATE/UPDATE/DELETE show different inputs.

## Import in Camunda Modeler

1. Open Camunda Modeler.
2. Go to settings/preferences for element templates.
3. Add this folder (or copy JSON files into your Modeler element-template path).
4. Restart Modeler if templates do not appear immediately.

## Common Modeling Notes

- Keep job type strings exactly aligned with worker polling job types.
- Prefer keeping default output variable names unless your BPMN model intentionally remaps them.
- If you remap output targets, update process expressions accordingly.
- For account search compatibility, worker-side alias handling exists (`customerId`, `customerID`, `customer_id`, nested `arguments`), so templates can stay strict while runtime remains resilient.

## Troubleshooting

- Template not visible: confirm valid JSON and restart Modeler.
- Task not executed: verify service task uses the correct template and job type.
- Empty results: confirm required inputs for the selected mode were actually provided.
- Unexpected variable names: check template output target mappings (especially `search-employee`, which defaults to `employeeSearchResponse`).
