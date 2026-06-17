# Camunda 8 Search Internal Systems Worker

A comprehensive Spring Boot application that provides Camunda 8 job workers for searching internal company systems including employees, customers, and external companies. This project includes both fuzzy matching capabilities and element templates for easy integration with Camunda 8 processes.

## 🎯 Why this project is useful for PoCs

This project is designed to act as a **ready-to-use source of external workers and data objects** for Camunda 8 process PoCs.

- It gives you runnable workers that behave like external systems (employee, customer, company, account search)
- It provides seeded domain objects (customers, employees, companies, accounts, links) so process flows can be tested quickly
- It includes connector templates so BPMN modelers can configure workers without writing code
- It exposes REST + dashboard views so teams can inspect and demo behavior during workshops and early discovery

In short: use this repository to simulate real integrations early, then replace each mocked/seeded data source with real upstream systems as your PoC matures.

## 🔗 What are Camunda 8 Connectors?

Camunda 8 Connectors are pre-built integrations that allow you to connect your BPMN processes with external systems, APIs, and services. They come in two main types:

### **Outbound Connectors (Job Workers)**
- Execute tasks that call external systems from your BPMN process
- Implemented as job workers that poll for jobs and execute business logic
- This project implements **custom outbound connectors** as Spring Boot job workers

### **Inbound Connectors**  
- Trigger BPMN process instances from external events
- Not implemented in this project

### **Element Templates**
- JSON configuration files that provide user-friendly forms in Camunda Modeler
- Allow process designers to configure connector parameters without writing code
- This project includes comprehensive element templates for all implemented connectors

## 🔍 Implemented Connectors

This project implements fourteen custom job workers with fuzzy matching and CRUD capabilities:

### 1. **Match Customer with DRI** (`match-customer-with-dri`)
- **Purpose**: Match customers with their designated relationship individuals (DRIs)
- **Search By**: Customer ID, Customer Name
- **Features**: Fuzzy matching, exact matching, single/multiple results
- **Returns**: Customer details + associated employee (DRI) information

### 2. **Search Employee** (`search-employee`)  
- **Purpose**: Search for employees across the organization
- **Search By**: Employee name, department, job title
- **Features**: Fuzzy matching, exact matching, partial name matching
- **Returns**: Employee details including ID, name, department, title, phone

### 3. **Query for Company** (`query-for-company`)
- **Purpose**: Search external companies database
- **Search By**: Company name, industry, city, revenue  
- **Features**: Fuzzy matching with company names and addresses
- **Returns**: Company details including ID, name, address, contact information

### 4. **Search Account** (`search-account`)
- **Purpose**: Search account records and customer-account relationships
- **Search By**: Account ID, legacy `customerAccountId` alias, account number, customer ID
- **Features**: Multi-result search, role mapping (OWNER/ADMIN/NAMED), fallback handling when `customerId` is interpreted as account id for compatibility
- **Returns**: Account details (type, balance, currency, status, dates) plus linked customer roles

### 5. **Manage Customer Record** (`manage-customer-record`)
- **Purpose**: Create, update, or delete customer records
- **Operation**: `CREATE`, `UPDATE`, `DELETE`
- **Features**: Customer ID auto-generation on CREATE (when omitted), trust level support (`L1`, `L2`, `L3`), and employee relationship validation
- **Returns**: Operation status + consolidated `customerCrudResult`

### 6. **Manage Employee Record** (`manage-employee-record`)
- **Purpose**: Create, update, or delete employee records
- **Operation**: `CREATE`, `UPDATE`, `DELETE`
- **Features**: Employee ID auto-generation on CREATE (when omitted), partial updates, and consistent operation result payloads
- **Returns**: Operation status + consolidated `employeeCrudResult`

### 7. **Manage Company Record** (`manage-company-record`)
- **Purpose**: Create, update, or delete external company records
- **Operation**: `CREATE`, `UPDATE`, `DELETE`
- **Features**: Company ID auto-generation on CREATE (when omitted), partial updates, and not-found signaling for update/delete
- **Returns**: Operation status + consolidated `companyCrudResult`

### 8. **Manage Account Record** (`manage-account-record`)
- **Purpose**: Create, update, or delete account records
- **Operation**: `CREATE`, `UPDATE`, `DELETE`
- **Features**: Account ID auto-generation on CREATE (when omitted), account type support (`SAVINGS`, `CORPORATE`, `PERSONAL`), and date/balance field handling
- **Returns**: Operation status + consolidated `accountCrudResult`

### 9. **Manage Customer Account Link** (`manage-customer-account-link`)
- **Purpose**: Connect customers to accounts, update existing customer roles on an account, or remove a link
- **Operation**: `UPSERT_ROLE`, `REMOVE_LINK`
- **Features**: Upsert semantics for relationship roles (`OWNER`, `ADMIN`, `NAMED`)
- **Returns**: Operation status + consolidated `customerAccountLinkResult`

### 10. **Manage Insurance Policy** (`manage-insurance-policy`)
- **Purpose**: Create, update, delete, or query insurance policies held by customers or external companies
- **Operation**: `CREATE`, `UPDATE`, `DELETE`, `QUERY`
- **Policy Types**: `LONG_TERM_INJURY`, `HOME`, `CAR`, `PET`
- **Holder Types**: `CUSTOMER`, `EXTERNAL_COMPANY`
- **Features**: Type-specific fields appear per policy type (CAR: vehicle registration/make/model/year; HOME: property address/type/value; PET: name/species/breed/age; LONG_TERM_INJURY: monthly benefit/waiting period/max benefit period); policy ID auto-generated on CREATE when omitted; defaults to `ACTIVE` status
- **Returns**: Operation status + consolidated `policyCrudResult`

### 11. **Manage Package** (`manage-package`)
- **Purpose**: Create, update, delete, or query logistics packages
- **Operation**: `CREATE`, `UPDATE`, `DELETE`, `QUERY`
- **Service Levels**: `ECONOMY`, `STANDARD`, `EXPRESS`, `OVERNIGHT`
- **Status Values**: `CREATED`, `SHIPPED`, `IN_TRANSIT`, `DELIVERED`, `RETURNED`
- **Features**: Tracks sender/recipient, origin/destination addresses, weight, shipping cost, and delivery dates; package ID auto-generated on CREATE when omitted; defaults to `CREATED` status
- **Returns**: Operation status + consolidated `packageCrudResult` (includes `trackingNumber`)

### 12. **Manage Product** (`manage-product`)
- **Purpose**: Create, update, delete, or query sellable products in the catalog
- **Operation**: `CREATE`, `UPDATE`, `DELETE`, `QUERY`
- **Features**: Tracks SKU, category, manufacturer, RRP, unit cost, stock quantity, and tier; `margin` is computed automatically as `rrp - unitCost`; product ID auto-generated on CREATE when omitted
- **Returns**: Operation status + consolidated `productCrudResult` (includes `sku`)

### 13. **Manage Purchase Order** (`manage-purchase-order`)
- **Purpose**: Create, update, delete, or query purchase order headers
- **Operation**: `CREATE`, `UPDATE`, `DELETE`, `QUERY`
- **Status Values**: `DRAFT`, `SUBMITTED`, `APPROVED`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- **Features**: Tracks customer, order date, expected delivery date, and notes; order total is computed automatically from its line items; order ID auto-generated on CREATE when omitted; defaults to `DRAFT` status
- **Returns**: Operation status + consolidated `orderCrudResult` (includes `orderNumber` and embedded items)
- **Note**: Line items are managed separately using `manage-purchase-item`

### 14. **Manage Purchase Item** (`manage-purchase-item`)
- **Purpose**: Add, update, remove, or query a single line item on an existing purchase order
- **Operation**: `CREATE`, `UPDATE`, `DELETE`, `QUERY`
- **Features**: `unitPrice` and `productName` default from the product catalog when omitted; `lineTotal` is computed as `quantity × unitPrice`; the parent order's `totalAmount` is recomputed automatically after every change; QUERY accepts either `purchaseItemId` (single item) or `orderId` (all items on an order)
- **Returns**: Operation status + consolidated `itemCrudResult`

### 🧾 **Worker Reference (at a glance)**

| Job Type | Worker Class | Primary Output Variable | Status Field |
|---|---|---|---|
| `match-customer-with-dri` | `MatchCustomerWithDriWorker` | `matchingResult` | `matchStatus` |
| `search-employee` | `EmployeeSearchWorker` | `employeeSearchResult` | `searchStatus` |
| `query-for-company` | `QueryForCompanyWorker` | `companySearchResult` | `status` inside `companySearchResult` |
| `search-account` | `AccountSearchWorker` | `accountSearchResult` | `searchStatus` |
| `manage-customer-record` | `CustomerCrudWorker` | `customerCrudResult` | `operationStatus` |
| `manage-employee-record` | `EmployeeCrudWorker` | `employeeCrudResult` | `operationStatus` |
| `manage-company-record` | `CompanyCrudWorker` | `companyCrudResult` | `operationStatus` |
| `manage-account-record` | `AccountCrudWorker` | `accountCrudResult` | `operationStatus` |
| `manage-customer-account-link` | `CustomerAccountLinkWorker` | `customerAccountLinkResult` | `operationStatus` |
| `manage-insurance-policy` | `InsurancePolicyCrudWorker` | `policyCrudResult` | `operationStatus` |
| `manage-package` | `PackageCrudWorker` | `packageCrudResult` | `operationStatus` |
| `manage-product` | `ProductCrudWorker` | `productCrudResult` | `operationStatus` |
| `manage-purchase-order` | `PurchaseOrderCrudWorker` | `orderCrudResult` | `operationStatus` |
| `manage-purchase-item` | `PurchaseItemCrudWorker` | `itemCrudResult` | `operationStatus` |

### 🧠 **Fuzzy Matching Features**
All connectors support advanced fuzzy matching using:
- **Levenshtein Distance Algorithm**: Calculates string similarity (70% threshold)
- **Word-based Matching**: Matches individual words within names/titles
- **Partial String Matching**: Finds substrings and partial matches
- **Configurable**: Enable/disable via dropdown in element templates

## 📋 Prerequisites

### System Requirements
- **Java 21** or higher
- **Maven 3.6** or higher
- **Camunda 8 SaaS Account** (free tier available)

### Development Tools (Recommended)
- **Camunda Modeler** - For designing BPMN processes and using element templates
- **IDE** - IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git** - For version control

### Camunda 8 Account Setup
1. Sign up at [camunda.com](https://camunda.com)
2. Create a Camunda 8 SaaS cluster
3. Generate API credentials (see configuration section below)

## ⚙️ Configuration Setup

### Step 1: Obtain Camunda 8 Connection Parameters

1. **Log into Camunda 8 Console**: 
   - Go to [console.camunda.io](https://console.camunda.io)
   - Sign in with your account

2. **Create/Select a Cluster**:
   - Click "Clusters" in the left navigation
   - Create a new cluster or select existing one
   - Note the **Cluster ID** and **Region**

3. **Generate API Client Credentials**:
   - In your cluster, go to "API" tab
   - Click "Create new client"
   - Select scopes: `Zeebe`, `Tasklist`, `Operate`
   - Copy the generated **Client ID** and **Client Secret**

### Step 2: Configure secrets (recommended: local gitignored file)

Create a local `application-secrets.properties` in the repository root (this file is gitignored):

```properties
camunda.client.auth.client-id=YOUR_CLIENT_ID_HERE
camunda.client.auth.client-secret=YOUR_CLIENT_SECRET_HERE
camunda.client.cloud.cluster-id=YOUR_CLUSTER_ID_HERE
camunda.client.cloud.region=YOUR_REGION_HERE
```

`src/main/resources/application.properties` imports this file via:

```properties
spring.config.import=optional:file:./application-secrets.properties
```

### Step 3: Environment variables (alternative)

You can also provide credentials using environment variables:

```powershell
$env:CAMUNDA_CLIENT_ID="your-client-id"
$env:CAMUNDA_CLIENT_SECRET="your-client-secret"
$env:CAMUNDA_CLUSTER_ID="your-cluster-id"
$env:CAMUNDA_REGION="your-region"
```

### Common Regions:
- `bru-2` (Europe - Belgium)
- `gcp-us-central1` (US Central)
- `aus-1` (Australia)

## 🚀 Getting Started

### 1. Clone and Build

```bash
git clone <repository-url>
cd search-internal-systems-worker
mvn clean compile
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8081`

### 3. Verify Connection

- Check logs for successful Camunda connection
- Visit `http://localhost:8081/actuator/health` for health status
- Access H2 console at `http://localhost:8081/h2-console` for database inspection

### 4. Import Element Templates

1. Copy all files from `element-templates/` directory
2. In Camunda Modeler:
   - Go to **File > Preferences > Element Templates**
   - Click **Add** and select the template JSON files
   - Templates will appear in the properties panel when designing processes

## 📊 Features

### Data Management
- **Persistent H2 Database**: Data survives application restarts
- **Deterministic Data Seeding**:
   - `DataSeedingService` ensures baseline employees/customers/companies
   - `DataInitializer` backfills customer trust/email and seeds accounts + links whenever missing
   - Optional full reset at startup via `app.seed.reset-on-startup=true`
- **Typical seeded volume**:
   - Employees: 50+
   - Customers: 100+
   - Companies: 100+
   - Accounts: 110 with customer-account relationship links

### REST API Endpoints
- `GET /api/customers` - List all customers
- `GET /api/customers/{id}` - Get customer by ID
- `POST /api/customers` - Create customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer
- `GET /api/employees` - List all employees  
- `GET /api/employees/{id}` - Get employee by ID
- `POST /api/employees` - Create employee
- `PUT /api/employees/{id}` - Update employee
- `DELETE /api/employees/{id}` - Delete employee
- `GET /api/companies` - List all companies
- `GET /api/companies/{id}` - Get company by ID
- `POST /api/companies` - Create company
- `PUT /api/companies/{id}` - Update company
- `DELETE /api/companies/{id}` - Delete company
- `GET /api/accounts` - List all accounts
- `GET /api/accounts/{id}` - Get account by ID
- `POST /api/accounts` - Create account
- `PUT /api/accounts/{id}` - Update account
- `DELETE /api/accounts/{id}` - Delete account
- `GET /api/customer-accounts` - List customer-account links
- `POST /api/customer-accounts` - Create link or upsert role (`accountId`, `customerId`, `role`)
- `PUT /api/customer-accounts` - Update link role (also supports upsert) (`accountId`, `customerId`, `role`)
- `DELETE /api/customer-accounts/{accountId}/{customerId}` - Remove customer-account link
- `GET /api/connection-status` - Camunda connectivity status
- `GET /api/worker-status` - Check job worker status
- `GET /api/job-history` - Recent job execution history
- `GET /api/job-metrics` - Aggregated job metrics
- `GET /actuator/health` - Application health check

## ▶️ Runbook (exactly how to run)

1. Ensure Java 17+ and Maven are installed.
2. Add credentials in local `application-secrets.properties` (or set env vars).
3. Build once:

```bash
mvn clean compile
```

4. Start the app:

```bash
mvn spring-boot:run
```

5. Verify:
   - Dashboard: `http://localhost:8081`
   - Health: `http://localhost:8081/actuator/health`
   - H2 console: `http://localhost:8081/h2-console`

## 🛠️ Maintenance guide

For repeatable field additions on existing entities, use:
- `docs/add-entity-variable-playbook.md`

### Day-to-day
- Keep `application-secrets.properties` local only (never commit)
- Review worker health using `GET /api/worker-status`
- Review job execution history in dashboard/API

### Data lifecycle
- The app always ensures customer trust/email fields are backfilled
- The app seeds accounts and customer-account links whenever those tables are empty
- Other seed datasets are created when below baseline thresholds
- To force a full reset/re-seed on startup, set `app.seed.reset-on-startup=true`
- If you need a clean local DB, run the VS Code task `Reset H2 DB files` or delete `data/camunda-worker-db.*`
- In PowerShell, manual reset is:

```powershell
Remove-Item -Force -ErrorAction SilentlyContinue "data/camunda-worker-db.mv.db", "data/camunda-worker-db.trace.db"
```

### Safe public repo practice
- Rotate Camunda credentials if they were ever committed
- Run a secrets scan before push (for example gitleaks)
- Stage selectively (`git add <files>`) instead of `git add .`

## 🧩 How to add a new worker (extension checklist)

For every new worker, update **all** of the following areas:

1. **Datasource / Domain Model**
   - Add or extend JPA entities in `src/main/java/com/example/camunda/model/`
   - Add repositories in `src/main/java/com/example/camunda/repository/`
   - Seed representative PoC data in `DataInitializer`

2. **Backend Service + Worker**
   - Add business logic in `src/main/java/com/example/camunda/service/`
   - Implement the worker in `src/main/java/com/example/camunda/worker/` (input validation, result mapping, error handling)
   - Expose/adjust REST endpoints in controllers if the dashboard needs new data views

3. **Polling Registration**
   - Register the new job type in `ZeebeJobPollingService` with a scheduled poll method
   - Ensure the job type string matches BPMN + element template exactly

4. **Frontend (Dashboard/UI)**
   - Add table/view/form wiring in `src/main/resources/templates/dashboard.html`
   - Add API fetch/render behavior for new entity/worker status and any create/update flows

5. **Connector Template**
   - Add a new JSON template under `element-templates/`
   - Define task type, inputs, outputs, and user-facing descriptions
   - Import template in Camunda Modeler and validate mapping against worker outputs

6. **Tests**
   - Add unit/integration tests for service + worker + controller paths
   - Add positive, not-found, and invalid-input scenarios

### Definition of done for a new worker
- Worker polls and handles jobs successfully
- Template appears and works in Modeler
- Dashboard/API show corresponding domain data
- Seed data supports realistic PoC demos
- Tests pass

### Monitoring & Management
- **Spring Boot Actuator**: Health checks and metrics
- **Comprehensive Logging**: Detailed job execution logs
- **H2 Console**: Database inspection and querying
- **Connection Status**: Real-time Camunda connection monitoring

## 🔧 Usage in BPMN Processes

### 1. Design Your Process
- Open Camunda Modeler
- Create a new BPMN diagram
- Add Service Tasks where you need data lookups

### 2. Configure Service Tasks
- Select a Service Task
- In Properties Panel, choose template:
  - "Match Customer with DRI"  
  - "Search Employee"
  - "Query for Company"
   - "Search Account"
  - "Manage Customer Record"
  - "Manage Employee Record"
  - "Manage Company Record"
  - "Manage Account Record"
  - "Manage Customer Account Link"
  - "Manage Insurance Policy"
  - "Manage Package"
  - "Manage Product"
  - "Manage Purchase Order"
  - "Manage Purchase Item"
- Fill in search parameters using static values or FEEL expressions

### 3. Configure Output Variables
Each connector now uses a single consolidated output variable:
- **Employee Search**: `employeeSearchResult` 
- **Customer Match**: `matchingResult`
- **Company Query**: `companySearchResult`
- **Account Search**: `accountSearchResult`
- **Customer CRUD**: `customerCrudResult`
- **Employee CRUD**: `employeeCrudResult`
- **Company CRUD**: `companyCrudResult`
- **Account CRUD**: `accountCrudResult`
- **Customer-Account Link CRUD**: `customerAccountLinkResult`
- **Insurance Policy CRUD**: `policyCrudResult`
- **Package CRUD**: `packageCrudResult`
- **Product CRUD**: `productCrudResult`
- **Purchase Order CRUD**: `orderCrudResult`
- **Purchase Item CRUD**: `itemCrudResult`

### 4. Deploy and Execute
- Deploy your process to Camunda 8
- Start process instances
- Job workers will automatically execute your service tasks

## 📝 Example FEEL Expressions

```javascript
// Static values
customerName: "Acme Corporation"
employeeName: "John Smith"

// Process variables  
customerId: =customer.id
employeeName: =request.employeeName
department: =searchCriteria.department

// Conditional expressions
fuzzyMatching: =if employee.exactMatch then false else true
allowMultiple: =count(customerIds) > 1
```

## 🔍 Troubleshooting

### Connection Issues
1. Verify cluster is running in Camunda Console
2. Check client credentials are correct and active
3. Ensure region matches your cluster's region
4. Check network connectivity and firewall settings

### Job Worker Issues  
1. Check application logs for errors
2. Verify job types match BPMN service task configurations
3. Ensure element templates are properly imported
4. Check database connectivity and data seeding

### Local H2 Startup Issues
If startup fails with H2 schema/connection errors (for example after entity changes):
1. Stop all running app instances
2. Delete local H2 files (`data/camunda-worker-db.*`)
3. Start the app again so schema + seeders recreate cleanly

### Performance Tips
1. Use exact matching when possible (faster than fuzzy)
2. Provide specific search criteria to reduce result sets
3. Monitor application logs for slow queries
4. Consider database indexing for large datasets

## 📚 Documentation

- **Element Templates**: See `element-templates/README.md` for detailed template documentation
- **API Documentation**: Available at runtime via Spring Boot Actuator
- **Camunda 8 Documentation**: [docs.camunda.io](https://docs.camunda.io)
- **FEEL Expression Guide**: [Camunda FEEL Documentation](https://docs.camunda.io/docs/components/modeler/feel/what-is-feel/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Built with ❤️ for Camunda 8 Integration**