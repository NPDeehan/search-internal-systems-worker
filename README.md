# Camunda 8 Search Internal Systems Worker

A comprehensive Spring Boot application that provides Camunda 8 job workers for searching internal company systems including employees, customers, and external companies. This project includes both fuzzy matching capabilities and element templates for easy integration with Camunda 8 processes.

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

This project implements three custom job workers with fuzzy matching capabilities:

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
- **Search By**: Company name, company address  
- **Features**: Fuzzy matching with company names and addresses
- **Returns**: Company details including ID, name, address, contact information

### 🧠 **Fuzzy Matching Features**
All connectors support advanced fuzzy matching using:
- **Levenshtein Distance Algorithm**: Calculates string similarity (70% threshold)
- **Word-based Matching**: Matches individual words within names/titles
- **Partial String Matching**: Finds substrings and partial matches
- **Configurable**: Enable/disable via dropdown in element templates

## 📋 Prerequisites

### System Requirements
- **Java 17** or higher
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

### Step 2: Configure Application Properties

Update `src/main/resources/application.properties`:

```properties
# Camunda 8 SaaS Connection
camunda.client.mode=saas
camunda.client.auth.client-id=YOUR_CLIENT_ID_HERE
camunda.client.auth.client-secret=YOUR_CLIENT_SECRET_HERE  
camunda.client.cloud.cluster-id=YOUR_CLUSTER_ID_HERE
camunda.client.cloud.region=YOUR_REGION_HERE
```

### Step 3: Environment Variables (Alternative/Secure)

Instead of hardcoding credentials, use environment variables:

```properties
camunda.client.auth.client-id=${CAMUNDA_CLIENT_ID}
camunda.client.auth.client-secret=${CAMUNDA_CLIENT_SECRET}
camunda.client.cloud.cluster-id=${CAMUNDA_CLUSTER_ID}
camunda.client.cloud.region=${CAMUNDA_REGION}
```

Set environment variables:
```bash
export CAMUNDA_CLIENT_ID="your-client-id"
export CAMUNDA_CLIENT_SECRET="your-client-secret"  
export CAMUNDA_CLUSTER_ID="your-cluster-id"
export CAMUNDA_REGION="your-region"
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

The application will start on `http://localhost:8080`

### 3. Verify Connection

- Check logs for successful Camunda connection
- Visit `http://localhost:8080/actuator/health` for health status
- Access H2 console at `http://localhost:8080/h2-console` for database inspection

### 4. Import Element Templates

1. Copy all files from `element-templates/` directory
2. In Camunda Modeler:
   - Go to **File > Preferences > Element Templates**
   - Click **Add** and select the template JSON files
   - Templates will appear in the properties panel when designing processes

## 📊 Features

### Data Management
- **Persistent H2 Database**: Data survives application restarts
- **Smart Data Seeding**: Auto-populates sample data only if database is empty
- **50 Sample Employees**: Various departments and job titles
- **100 Sample Customers**: With assigned employee relationships  
- **100 Sample Companies**: External company directory

### REST API Endpoints
- `GET /api/customers` - List all customers
- `GET /api/employees` - List all employees  
- `GET /api/companies` - List all companies
- `GET /api/worker-status` - Check job worker status
- `GET /actuator/health` - Application health check

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
- Fill in search parameters using static values or FEEL expressions

### 3. Configure Output Variables
Each connector now uses a single consolidated output variable:
- **Employee Search**: `employeeSearchResponse` 
- **Customer Match**: `matchingResult`
- **Company Query**: `companySearchResponse`

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