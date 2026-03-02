package com.example.camunda;

import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.AccountType;
import com.example.camunda.model.Customer;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.repository.AccountRepository;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.CustomerAccountRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.repository.ExternalCompanyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initData(CustomerRepository customerRepo,
                               EmployeeRepository employeeRepo,
                               ExternalCompanyRepository companyRepo,
                               AccountRepository accountRepo,
                               CustomerAccountRepository customerAccountRepo,
                               @Value("${app.seed.reset-on-startup:false}") boolean resetOnStartup) {
        return args -> {
            boolean hasExistingData =
                    customerRepo.count() > 0 ||
                    employeeRepo.count() > 0 ||
                    companyRepo.count() > 0 ||
                    accountRepo.count() > 0 ||
                    customerAccountRepo.count() > 0;

            if (hasExistingData && !resetOnStartup) {
                return;
            }

            if (resetOnStartup) {
                customerAccountRepo.deleteAll();
                accountRepo.deleteAll();
                customerRepo.deleteAll();
                employeeRepo.deleteAll();
                companyRepo.deleteAll();
            }

            List<Employee> employees = seedEmployees(employeeRepo);
            List<Customer> customers = seedCustomers(customerRepo, employees);
            seedExternalCompanies(companyRepo);
            seedAccountsAndLinks(accountRepo, customerAccountRepo, customers);
        };
    }

    private List<Employee> seedEmployees(EmployeeRepository employeeRepo) {
        List<Employee> employees = new ArrayList<>();

        employees.add(createEmployee(1L, "Alice Smith", "Account Manager", "Sales", "123-456-7890"));
        employees.add(createEmployee(2L, "Bob Johnson", "Support Lead", "Support", "987-654-3210"));
        employees.add(createEmployee(3L, "Carol White", "Customer Success Manager", "Customer Success", "555-100-3000"));
        employees.add(createEmployee(4L, "David Brown", "Relationship Director", "Enterprise Banking", "555-100-4000"));
        employees.add(createEmployee(5L, "Emma Green", "Account Specialist", "Retail Banking", "555-100-5000"));

        String[] euFirstNames = {"Luca", "Marta", "Sven", "Elena", "Tomas", "Sofia", "Katarzyna", "Niels", "Aoife", "Matteo"};
        String[] euLastNames = {"Rossi", "Kowalski", "Novak", "Muller", "Dubois", "Jensen", "Silva", "Popescu", "Garcia", "Santos"};
        String[] nonEuFirstNames = {"Ethan", "Maya", "Noah", "Aisha", "Liam", "Aarav"};
        String[] nonEuLastNames = {"Wilson", "Chen", "Khan", "Taylor", "Nguyen", "Patel"};
        String[] departments = {"Retail Banking", "Corporate Banking", "Support", "Fraud", "Wealth", "Compliance"};
        String[] jobTitles = {"Account Specialist", "Relationship Manager", "Support Analyst", "Portfolio Officer", "Customer Advisor", "Risk Analyst"};

        for (int i = 0; i < 100; i++) {
            boolean european = i < 70;
            String first = european ? euFirstNames[i % euFirstNames.length] : nonEuFirstNames[i % nonEuFirstNames.length];
            String last = european ? euLastNames[(i * 3) % euLastNames.length] : nonEuLastNames[(i * 2) % nonEuLastNames.length];
            long id = 1000L + i;
            String phone = String.format("+%s-%03d-%04d", european ? "353" : "1", (i % 900) + 100, (i * 37 % 9000) + 1000);

            employees.add(createEmployee(
                    id,
                    first + " " + last,
                    jobTitles[i % jobTitles.length],
                    departments[i % departments.length],
                    phone
            ));
        }

        return employeeRepo.saveAll(employees);
    }

    private List<Customer> seedCustomers(CustomerRepository customerRepo, List<Employee> employees) {
        List<Customer> customers = new ArrayList<>();

        customers.add(createCustomer(100L, "Niall Deehan", "14 Fitzwilliam Square, Dublin, Ireland", 1L));
        customers.add(createCustomer(150L, "John Doe", "77 River Lane, Cork, Ireland", 2L));
        customers.add(createCustomer(200L, "Sarah Connor", "22 Rue Saint-Honore, Paris, France", 2L));
        customers.add(createCustomer(300L, "James Patel", "9 Unter den Linden, Berlin, Germany", 3L));
        customers.add(createCustomer(400L, "Olivia Murphy", "31 Gran Via, Madrid, Spain", 4L));
        customers.add(createCustomer(500L, "Liam O'Connor", "8 Via Roma, Milan, Italy", 5L));
        customers.add(createCustomer(600L, "Ava Thompson", "56 Damrak, Amsterdam, Netherlands", 1L));

        String[] euFirstNames = {"Anna", "Marco", "Isla", "Gregor", "Ines", "Pavel", "Maeve", "Jonas", "Clara", "Eoin"};
        String[] euLastNames = {"Schmidt", "Costa", "Doyle", "Nowak", "Van Dijk", "Petrov", "Larsen", "Moreno", "Bianchi", "Ivanova"};
        String[] nonEuFirstNames = {"Aiden", "Priya", "Yuki", "Daniel", "Leila", "Omar"};
        String[] nonEuLastNames = {"Singh", "Kim", "Anderson", "Hassan", "Lopez", "Wong"};
        String[] euCities = {"Dublin", "Paris", "Berlin", "Madrid", "Milan", "Warsaw", "Amsterdam", "Prague", "Lisbon", "Copenhagen"};
        String[] euCountries = {"Ireland", "France", "Germany", "Spain", "Italy", "Poland", "Netherlands", "Czechia", "Portugal", "Denmark"};
        String[] nonEuCities = {"New York", "Toronto", "Singapore", "Sydney", "Tokyo", "Dubai"};
        String[] nonEuCountries = {"USA", "Canada", "Singapore", "Australia", "Japan", "UAE"};

        for (int i = 0; i < 100; i++) {
            boolean european = i < 70;
            String first = european ? euFirstNames[i % euFirstNames.length] : nonEuFirstNames[i % nonEuFirstNames.length];
            String last = european ? euLastNames[(i * 5) % euLastNames.length] : nonEuLastNames[(i * 3) % nonEuLastNames.length];
            long id = 2000L + i;
            long employeeId = employees.get(i % employees.size()).getEmployeeId();
            String city = european ? euCities[i % euCities.length] : nonEuCities[i % nonEuCities.length];
            String country = european ? euCountries[i % euCountries.length] : nonEuCountries[i % nonEuCountries.length];
            String address = (100 + i) + " " + (european ? "High Street" : "Market Street") + ", " + city + ", " + country;
            customers.add(createCustomer(id, first + " " + last, address, employeeId));
        }

        return customerRepo.saveAll(customers);
    }

    private void seedExternalCompanies(ExternalCompanyRepository companyRepo) {
        List<ExternalCompany> companies = new ArrayList<>();

        companies.add(createCompany(1000L, "Globex Inc", "1 Main St, Metropolis", "Jane Doe", "555-111-2222"));
        companies.add(createCompany(1500L, "Fustion Technologies", "88 Innovation Park, Dublin", "Alex Murphy", "555-444-1212"));
        companies.add(createCompany(2000L, "Initech", "42 Silicon Ave, Tech City", "John Roe", "555-333-4444"));
        companies.add(createCompany(3000L, "Umbrella Financial", "17 Queen Rd, Dublin", "Martha Lane", "555-222-8888"));

        String[] euCities = {"Dublin", "Paris", "Berlin", "Madrid", "Milan", "Warsaw", "Amsterdam", "Prague", "Lisbon", "Copenhagen"};
        String[] euCountries = {"Ireland", "France", "Germany", "Spain", "Italy", "Poland", "Netherlands", "Czechia", "Portugal", "Denmark"};
        String[] nonEuCities = {"New York", "Toronto", "Singapore", "Sydney", "Tokyo"};
        String[] nonEuCountries = {"USA", "Canada", "Singapore", "Australia", "Japan"};
        String[] euNameLeads = {"Euro", "Nordic", "Alpine", "Atlantic", "Continental", "Union", "Prime", "Summit"};
        String[] nonEuNameLeads = {"Global", "Pacific", "Meridian", "Vertex", "Harbor", "Pioneer", "Frontier", "Nexus"};
        String[] nameMiddles = {"Capital", "Trade", "Finance", "Asset", "Wealth", "Growth", "Credit", "Market", "Advisory", "Bridge"};
        String[] nameEnds = {"Holdings", "Group", "Partners", "Ventures", "Solutions", "Advisors", "Services", "International"};

        for (int i = 0; i < 100; i++) {
            boolean european = i < 70;
            String city = european ? euCities[i % euCities.length] : nonEuCities[i % nonEuCities.length];
            String country = european ? euCountries[i % euCountries.length] : nonEuCountries[i % nonEuCountries.length];
            long id = 4000L + i;
            String lead = european
                    ? euNameLeads[i % euNameLeads.length]
                    : nonEuNameLeads[i % nonEuNameLeads.length];
            String middle = nameMiddles[(i * 3) % nameMiddles.length];
            String end = nameEnds[(i * 5) % nameEnds.length];
            String name = lead + " " + middle + " " + end;
            String address = (10 + i) + " Market Street, " + city + ", " + country;
            String contact = (european ? "Elena" : "Jordan") + " Contact " + (i + 1);
            String phone = String.format("+%s-%03d-%04d", european ? "44" : "1", (i % 900) + 100, (i * 29 % 9000) + 1000);
            companies.add(createCompany(id, name, address, contact, phone));
        }

        companyRepo.saveAll(companies);
    }

    private void seedAccountsAndLinks(AccountRepository accountRepo,
                                      CustomerAccountRepository customerAccountRepo,
                                      List<Customer> customers) {
        List<Account> accounts = new ArrayList<>();
        List<CustomerAccount> links = new ArrayList<>();

        accounts.add(createAccount(10001L, "SA-1001-NDL", AccountType.SAVINGS, "12500.50", "USD", "Active", "1.25", LocalDate.of(2020, 1, 15), LocalDate.of(2026, 2, 1)));
        accounts.add(createAccount(10002L, "PA-2002-SCN", AccountType.PERSONAL, "4820.75", "USD", "Active", "0.15", LocalDate.of(2021, 5, 20), LocalDate.of(2026, 1, 28)));
        accounts.add(createAccount(10003L, "CO-3003-OPS", AccountType.CORPORATE, "725000.00", "USD", "Active", "0.05", LocalDate.of(2019, 9, 10), LocalDate.of(2026, 2, 2)));
        accounts.add(createAccount(10004L, "SA-4004-JPT", AccountType.SAVINGS, "99000.10", "EUR", "Active", "1.80", LocalDate.of(2022, 3, 12), LocalDate.of(2026, 2, 5)));
        accounts.add(createAccount(10005L, "PA-5005-OMR", AccountType.PERSONAL, "1550.99", "GBP", "Dormant", "0.10", LocalDate.of(2018, 11, 1), LocalDate.of(2025, 4, 14)));
        accounts.add(createAccount(10006L, "CO-6006-LCO", AccountType.CORPORATE, "2500000.00", "USD", "Active", "0.03", LocalDate.of(2017, 7, 7), LocalDate.of(2026, 2, 6)));
        accounts.add(createAccount(10007L, "SA-7007-AVT", AccountType.SAVINGS, "7340.22", "EUR", "Active", "1.30", LocalDate.of(2024, 6, 3), LocalDate.of(2026, 2, 3)));
        accounts.add(createAccount(10008L, "PA-8008-SHR", AccountType.PERSONAL, "28350.00", "USD", "Active", "0.20", LocalDate.of(2023, 8, 19), LocalDate.of(2026, 2, 7)));
        accounts.add(createAccount(10009L, "CO-9009-MIX", AccountType.CORPORATE, "485000.55", "GBP", "Restricted", "0.04", LocalDate.of(2020, 12, 21), LocalDate.of(2026, 1, 20)));
        accounts.add(createAccount(10010L, "SA-1010-JNT", AccountType.SAVINGS, "310.42", "EUR", "Active", "1.10", LocalDate.of(2025, 1, 9), LocalDate.of(2026, 2, 8)));

        String[] euCurrencies = {"EUR", "GBP", "CHF"};
        String[] nonEuCurrencies = {"USD", "CAD", "SGD"};

        for (int i = 0; i < 100; i++) {
            boolean european = i < 70;
            long id = 20001L + i;
            AccountType type = switch (i % 3) {
                case 0 -> AccountType.SAVINGS;
                case 1 -> AccountType.PERSONAL;
                default -> AccountType.CORPORATE;
            };
            String prefix = type == AccountType.SAVINGS ? "SA" : type == AccountType.PERSONAL ? "PA" : "CO";
            String currency = european ? euCurrencies[i % euCurrencies.length] : nonEuCurrencies[i % nonEuCurrencies.length];
            String status = i % 13 == 0 ? "Dormant" : i % 17 == 0 ? "Restricted" : "Active";
            BigDecimal balance = type == AccountType.CORPORATE
                    ? BigDecimal.valueOf(250000L + (i * 9750L)).add(BigDecimal.valueOf(i * 0.37))
                    : BigDecimal.valueOf(1500L + (i * 630L)).add(BigDecimal.valueOf(i * 0.21));
            BigDecimal interest = type == AccountType.CORPORATE
                    ? BigDecimal.valueOf(0.02 + (i % 4) * 0.01)
                    : BigDecimal.valueOf(0.15 + (i % 6) * 0.20);

            Account account = new Account();
            account.setAccountId(id);
            account.setAccountNumber(String.format("%s-%05d-%s", prefix, id, european ? "EU" : "GL"));
            account.setAccountType(type);
            account.setBalance(balance);
            account.setCurrency(currency);
            account.setStatus(status);
            account.setInterestRate(interest);
            account.setOpenedDate(LocalDate.of(2017 + (i % 9), (i % 12) + 1, (i % 27) + 1));
            account.setLastActivityDate(LocalDate.of(2026, ((i + 3) % 12) + 1, ((i + 7) % 27) + 1));
            accounts.add(account);
        }

        accountRepo.saveAll(accounts);

        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            Customer owner = customers.get(i % customers.size());

            CustomerAccount ownerLink = new CustomerAccount();
            ownerLink.setCustomer(owner);
            ownerLink.setAccount(account);
            ownerLink.setRole(AccountRole.OWNER);
            links.add(ownerLink);

            if (i % 3 == 0) {
                Customer adminCustomer = customers.get((i + 7) % customers.size());
                if (!adminCustomer.getCustomerId().equals(owner.getCustomerId())) {
                    CustomerAccount adminLink = new CustomerAccount();
                    adminLink.setCustomer(adminCustomer);
                    adminLink.setAccount(account);
                    adminLink.setRole(AccountRole.ADMIN);
                    links.add(adminLink);
                }
            }

            if (i % 5 == 0) {
                Customer namedCustomer = customers.get((i + 13) % customers.size());
                if (!namedCustomer.getCustomerId().equals(owner.getCustomerId())) {
                    CustomerAccount namedLink = new CustomerAccount();
                    namedLink.setCustomer(namedCustomer);
                    namedLink.setAccount(account);
                    namedLink.setRole(AccountRole.NAMED);
                    links.add(namedLink);
                }
            }
        }

        customerAccountRepo.saveAll(links);
    }

    private Employee createEmployee(Long id, String name, String title, String department, String phone) {
        Employee employee = new Employee();
        employee.setEmployeeId(id);
        employee.setFullName(name);
        employee.setJobTitle(title);
        employee.setDepartment(department);
        employee.setPhoneNumber(phone);
        return employee;
    }

    private Customer createCustomer(Long id, String name, String address, Long employeeId) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setCustomerName(name);
        customer.setAddress(address);
        customer.setEmployeeId(employeeId);
        return customer;
    }

    private ExternalCompany createCompany(Long id, String name, String address, String contactPerson, String phone) {
        ExternalCompany company = new ExternalCompany();
        company.setCompanyId(id);
        company.setCompanyName(name);
        company.setAddress(address);
        company.setContactPerson(contactPerson);
        company.setPhoneNumber(phone);
        return company;
    }

    private Account createAccount(Long id,
                                  String accountNumber,
                                  AccountType type,
                                  String balance,
                                  String currency,
                                  String status,
                                  String interestRate,
                                  LocalDate openedDate,
                                  LocalDate lastActivityDate) {
        Account account = new Account();
        account.setAccountId(id);
        account.setAccountNumber(accountNumber);
        account.setAccountType(type);
        account.setBalance(new BigDecimal(balance));
        account.setCurrency(currency);
        account.setStatus(status);
        account.setInterestRate(new BigDecimal(interestRate));
        account.setOpenedDate(openedDate);
        account.setLastActivityDate(lastActivityDate);
        return account;
    }
}
