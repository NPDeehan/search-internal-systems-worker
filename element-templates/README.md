# Camunda 8 Element Templates (Enhanced with FEEL Support)

This directory contains enhanced element templates for the Camunda 8 Worker Dashboard project, now supporting FEEL expressions and consolidated output variables.

## 🆕 **Key Features**

- **FEEL Expression Support**: All input parameters now support both static values and FEEL expressions
- **Consolidated Output Variables**: Single output variable per template for easier process variable mapping
- **Fuzzy Matching Support**: Advanced similarity-based searching using Levenshtein distance algorithms
- **Null Parameter Handling**: Robust validation ensuring at least one search parameter is provided
- **Enhanced Error Handling**: Comprehensive validation and error reporting

## Available Templates

### 1. Match Customer with DRI (`match-customer-with-dri.json`)

**Purpose**: Service task template for matching customers with their designated relationship individuals (DRIs).

**Input Parameters** (with FEEL support):

- `customerId` (Integer): The customer ID to look up
  - Static value: `123`
  - FEEL expression: `=customerId`, `=request.id`, `=customer.id`
- `customerName` (Text): The customer name to search for (alternative to Customer ID)
  - Static value: `"Acme Corp"`
  - FEEL expression: `=customerName`, `=customer.name`
- `allowMultiple` (Dropdown, optional): Whether to allow multiple customer matches
  - `false`: Return single match (default)
  - `true`: Return multiple matches
- `fuzzyMatching` (Dropdown, optional): Enable fuzzy/similarity matching
  - `false`: Exact matching only (default)
  - `true`: Fuzzy matching using Levenshtein distance algorithm

**Output Variables**:

- `matchingResult`: Consolidated result object containing customer, employee, and match status
- `matchStatus`: Simple status indicator (SUCCESS, NOT_FOUND, ERROR)
- Customer-specific fields:
  - `customerId`: Customer ID
  - `customerName`: Customer name
  - `customerEmployeeId`: Associated employee ID
- Employee-specific fields (DRI details):
  - `employeeId`: Employee ID
  - `employeeName`: Employee full name
  - `employeeTitle`: Job title
  - `employeeDepartment`: Department
  - `employeePhone`: Phone number

**Task Type**: `match-customer-with-dri`

**Example Success Response**:

```json
{
  "status": "SUCCESS",
  "customer": {
    "customerId": 123,
    "customerName": "Acme Corp",
    "employeeId": 456
  },
  "employee": {
    "employeeId": 456,
    "fullName": "Jane Smith",
    "jobTitle": "Account Manager",
    "department": "Sales",
    "phoneNumber": "+1-555-0124"
  },
  "timestamp": "2025-09-01T07:54:00"
}
```

**Example Not Found Response**:

```json
{
  "status": "NOT_FOUND",
  "error": "Customer with ID 999 not found"
}
```

---

### 2. Query for Company (`query-for-company.json`)

**Purpose**: Service task template for searching external companies by various criteria.

**Input Parameters** (with FEEL support):

- `companyName` (Text, optional): The name of the company to search for
  - Static value: `"Tech Corp"`
  - FEEL expression: `=companyName`, `=request.company`, `=client.name`
- `industry` (Text, optional): Filter companies by industry
  - Static value: `"Technology"`
  - FEEL expression: `=industry`, `=sector`
- `location` (Text, optional): Filter companies by location
  - Static value: `"New York"`
  - FEEL expression: `=location`, `=city`
- `revenue` (Text, optional): Filter companies by minimum revenue
  - Static value: `1000000`
  - FEEL expression: `=revenue`, `=minRevenue`
- `fuzzyMatching` (Dropdown, optional): Enable fuzzy/similarity matching for company name and address
  - `false`: Exact matching only (default)
  - `true`: Fuzzy matching using Levenshtein distance algorithm

**Output Variables**:

- `companyQueryResult`: Consolidated result object containing company information
- `queryStatus`: Simple status indicator (SUCCESS, NOT_FOUND, VALIDATION_ERROR, ERROR)
- `companies`: List of found companies
- `companyCount`: Number of companies found
- Individual company fields (when exactly one company is found):
  - `companyId`: Company ID
  - `companyName`: Company name
  - `companyIndustry`: Industry
  - `companyLocation`: Location

**Task Type**: `query-for-company`

**Example Success Response**:

```json
{
  "status": "SUCCESS",
  "companyCount": 1,
  "companies": [
    {
      "companyId": 789,
      "name": "Tech Corp",
      "industry": "Technology",
      "location": "New York"
    }
  ],
  "timestamp": "2025-09-01T07:54:00"
}
```

**Example Not Found Response**:

```json
{
  "status": "NOT_FOUND",
  "error": "No companies found matching the specified criteria"
}
```

---

### 3. Search Employee (`search-employee.json`)

**Purpose**: Service task template for searching employees by name, department, or job title with flexible matching options.

**Input Parameters** (with FEEL support):

- `employeeName` (Text, optional): The name of the employee to search for
  - Static value: `"John Smith"`
  - FEEL expression: `=employeeName`, `=request.name`, `=user.fullName`
- `department` (Text, optional): Filter employees by department
  - Static value: `"Sales"`
  - FEEL expression: `=department`, `=request.dept`
- `jobTitle` (Text, optional): Filter employees by job title (partial match supported)
  - Static value: `"Manager"`
  - FEEL expression: `=jobTitle`, `=position`
- `exactMatch` (Dropdown, optional): Whether to require exact name matching
  - `false`: Allow partial matching (default)
  - `true`: Exact match only
- `fuzzyMatching` (Dropdown, optional): Enable advanced fuzzy/similarity matching for all search fields
  - `false`: Exact matching only (default)
  - `true`: Fuzzy matching using Levenshtein distance algorithm

**Output Variables**:

- `employeeSearchResult`: Consolidated result object containing search status, found employees, and metadata
- `searchStatus`: Simple status indicator (SUCCESS, NOT_FOUND, VALIDATION_ERROR, ERROR)
- `employees`: List of found employees with their details
- `employeeCount`: Number of employees found
- Individual employee fields (when exactly one employee is found):
  - `employeeId`: Employee ID
  - `employeeName`: Employee full name
  - `employeeTitle`: Job title
  - `employeeDepartment`: Department
  - `employeePhone`: Phone number

**Task Type**: `search-employee`

**Example Success Response**:

```json
{
  "status": "SUCCESS",
  "employeeCount": 1,
  "employees": [
    {
      "employeeId": 123,
      "fullName": "John Smith",
      "jobTitle": "Software Engineer",
      "department": "IT",
      "phoneNumber": "+1-555-0123"
    }
  ],
  "searchParameters": {
    "employeeName": "John Smith",
    "exactMatch": true
  },
  "timestamp": "2025-09-01T07:54:00"
}
```

**Example Not Found Response**:

```json
{
  "status": "NOT_FOUND",
  "error": "No employees found matching the specified criteria"
}
```

**Example Validation Error Response**:

```json
{
  "status": "VALIDATION_ERROR",
  "error": "At least one search parameter must be provided"
}
```

---

## Fuzzy Matching Feature

All templates now support **fuzzy matching** functionality that enables intelligent similarity-based searches using advanced algorithms:

### How Fuzzy Matching Works

- **Levenshtein Distance Algorithm**: Calculates character-level similarity between strings
- **70% Similarity Threshold**: Matches are considered valid if they have 70% or higher similarity
- **Multi-Field Support**: Applies to relevant text fields (names, addresses, departments, etc.)
- **Word-based Matching**: Also performs word-level partial matching for better results

### When to Use Fuzzy Matching

✅ **Recommended for**:
- User input with potential typos
- Partial name searches
- Handling data inconsistencies
- Flexible customer/employee searches

❌ **Not recommended for**:
- ID-based searches
- Exact compliance requirements
- Performance-critical operations with large datasets

### Examples

```javascript
// Customer search with fuzzy matching
{
  "customerName": "Jon Doe",      // User typed "Jon" instead of "John"
  "fuzzyMatching": true           // Will match "John Doe" with 85% similarity
}

// Employee search with fuzzy matching
{
  "employeeName": "Jane Smith",
  "department": "Sails",          // Typo in "Sales"
  "fuzzyMatching": true           // Will match "Sales" department
}

// Company search with fuzzy matching
{
  "companyName": "Acme Corp",     // Will match "ACME Corporation"
  "fuzzyMatching": true
}
```

---

## Usage Guide

### Setting Up Templates in Camunda Modeler

1. **Copy Template Files**: Place all `.json` template files in your Camunda Modeler's `element-templates` directory
2. **Open Camunda Modeler**: Restart the modeler to load the new templates
3. **Apply Templates**: Select a service task and choose the desired template from the template selector

### FEEL Expression Examples

```javascript
// Simple variable reference
=customerId

// Object property access
=request.customer.id

// Conditional expressions
=if customerId != null then customerId else 0

// String concatenation
=employeeName + " - " + department

// Default value handling
=if employeeName != null then employeeName else "Unknown"
```

### Process Variable Mapping

After task completion, access results using:

```javascript
// Full result object
=matchingResult

// Status check
=matchingResult.status

// Specific data
=matchingResult.customer.customerName
=employeeSearchResult.employees[1].fullName
```

## Error Handling

All templates provide consistent error handling with structured responses:

- **SUCCESS**: Operation completed successfully
- **NOT_FOUND**: No matching records found
- **VALIDATION_ERROR**: Invalid or missing input parameters
- **ERROR**: System or processing error

## Support and Troubleshooting

- Ensure all worker services are running and accessible
- Verify input parameter types match template expectations
- Check FEEL expression syntax using Camunda's FEEL playground
- Monitor worker logs for detailed error information
