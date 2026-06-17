package com.example.camunda;

import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.AccountType;
import com.example.camunda.model.Customer;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.model.InsurancePolicy;
import com.example.camunda.model.OrderStatus;
import com.example.camunda.model.Package;
import com.example.camunda.model.PackageStatus;
import com.example.camunda.model.Product;
import com.example.camunda.model.PurchaseItem;
import com.example.camunda.model.PurchaseOrder;
import com.example.camunda.model.PolicyHolderType;
import com.example.camunda.model.PolicyStatus;
import com.example.camunda.model.PolicyType;
import com.example.camunda.model.ServiceLevel;
import com.example.camunda.model.TrustLevel;
import com.example.camunda.repository.AccountRepository;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.CustomerAccountRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.repository.ExternalCompanyRepository;
import com.example.camunda.repository.InsurancePolicyRepository;
import com.example.camunda.repository.PackageRepository;
import com.example.camunda.repository.ProductRepository;
import com.example.camunda.repository.PurchaseOrderRepository;
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

    private static final String NIAL_DEEHAN_NAME = "Niall Deehan";
    private static final String NIAL_DEEHAN_EMAIL = "niall.deehan@camunda.com";

    private static final String[] GLOBAL_FIRST_NAMES = {
            "Aisha", "Aarav", "Mei", "Sofia", "Luka", "Nia", "Thiago", "Yara", "Omar", "Hana",
            "Kwame", "Elena", "Mateo", "Amira", "Jonas", "Anika", "Ravi", "Mina", "Noa", "Ibrahim"
    };
    private static final String[] GLOBAL_LAST_NAMES = {
            "Okafor", "Singh", "Kim", "Silva", "Fernandez", "Nakamura", "Hassan", "Kowalski", "Mensah", "Dubois",
            "Novak", "Garcia", "Patel", "Rahman", "Tanaka", "Santos", "Ali", "Ionescu", "Larsen", "Pereira"
    };
    private static final String[] GLOBAL_STREETS = {
            "Harbour Road", "King Street", "Liberty Avenue", "Market Lane", "Canal Road",
            "Station Road", "Olive Street", "Cedar Lane", "Sunrise Avenue", "Riverside Drive"
    };
    private static final CityCountry[] GLOBAL_LOCATIONS = {
            new CityCountry("Dublin", "Ireland"),
            new CityCountry("Lagos", "Nigeria"),
            new CityCountry("Mumbai", "India"),
            new CityCountry("Sao Paulo", "Brazil"),
            new CityCountry("Tokyo", "Japan"),
            new CityCountry("Seoul", "South Korea"),
            new CityCountry("Mexico City", "Mexico"),
            new CityCountry("Dubai", "UAE"),
            new CityCountry("Nairobi", "Kenya"),
            new CityCountry("Munich", "Germany"),
            new CityCountry("Copenhagen", "Denmark"),
            new CityCountry("Auckland", "New Zealand"),
            new CityCountry("Istanbul", "Turkiye"),
            new CityCountry("Cape Town", "South Africa"),
            new CityCountry("Santiago", "Chile")
    };
    private static final String[] COMPANY_NAME_PREFIXES = {
            "Aegis", "Atlas", "Nile", "Meridian", "Harbor", "Orchid", "Summit", "Aurora", "Pioneer", "Vertex"
    };
    private static final String[] COMPANY_NAME_MIDDLES = {
            "Capital", "Trade", "Finance", "Advisory", "Markets", "Growth", "Wealth", "Bridge", "Holdings", "Ventures"
    };
    private static final String[] COMPANY_NAME_SUFFIXES = {
            "Group", "Partners", "Solutions", "International", "Collective", "Network", "Services", "Associates"
    };
    private static final String[] CONTACT_FIRST_NAMES = {
            "Amina", "Kenji", "Lucia", "Diego", "Noor", "Marek", "Priya", "Tunde", "Elif", "Sora"
    };
    private static final String[] CONTACT_LAST_NAMES = {
            "Diallo", "Ito", "Costa", "Haddad", "Nowak", "Sharma", "Mensah", "Demir", "Park", "Silveira"
    };

    @Bean
    CommandLineRunner initData(CustomerRepository customerRepo,
                               EmployeeRepository employeeRepo,
                               ExternalCompanyRepository companyRepo,
                               AccountRepository accountRepo,
                               CustomerAccountRepository customerAccountRepo,
                               InsurancePolicyRepository insurancePolicyRepo,
                               PackageRepository packageRepo,
                               ProductRepository productRepo,
                               PurchaseOrderRepository purchaseOrderRepo,
                               @Value("${app.seed.reset-on-startup:false}") boolean resetOnStartup) {
        return args -> {
            backfillCustomerTrustLevels(customerRepo);
            backfillCustomerEmails(customerRepo);

            if (resetOnStartup) {
                purchaseOrderRepo.deleteAll();
                productRepo.deleteAll();
                packageRepo.deleteAll();
                insurancePolicyRepo.deleteAll();
                customerAccountRepo.deleteAll();
                accountRepo.deleteAll();
                customerRepo.deleteAll();
                employeeRepo.deleteAll();
                companyRepo.deleteAll();
            }

            List<Employee> employees = employeeRepo.count() == 0
                    ? seedEmployees(employeeRepo)
                    : employeeRepo.findAll();

            List<Customer> customers = customerRepo.count() == 0
                    ? seedCustomers(customerRepo, employees)
                    : customerRepo.findAll();

            List<ExternalCompany> companies;
            if (companyRepo.count() == 0) {
                seedExternalCompanies(companyRepo);
                companies = companyRepo.findAll();
            } else {
                companies = companyRepo.findAll();
            }

            // Always seed accounts/links when missing, even if employees/customers were seeded elsewhere.
            if (accountRepo.count() == 0 || customerAccountRepo.count() == 0) {
                customerAccountRepo.deleteAll();
                accountRepo.deleteAll();
                seedAccountsAndLinks(accountRepo, customerAccountRepo, customers);
            }

            if (insurancePolicyRepo.count() == 0) {
                seedInsurancePolicies(insurancePolicyRepo, customers, companies);
            }

            if (packageRepo.count() == 0) {
                seedPackages(packageRepo, customers);
            }

            List<Product> products = productRepo.count() == 0
                    ? seedProducts(productRepo)
                    : productRepo.findAll();

            if (purchaseOrderRepo.count() == 0) {
                seedPurchaseOrders(purchaseOrderRepo, products, customers);
            }
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

        customers.add(createCustomer(100L, "Niall Deehan", "14 Fitzwilliam Square, Dublin, Ireland", 1L, TrustLevel.L1, NIAL_DEEHAN_EMAIL));
        customers.add(createCustomer(150L, "John Doe", "77 River Lane, Cork, Ireland", 2L, TrustLevel.L2, buildFakeCustomerEmail("John Doe", 150L)));
        customers.add(createCustomer(200L, "Sarah Connor", "22 Rue Saint-Honore, Paris, France", 2L, TrustLevel.L3, buildFakeCustomerEmail("Sarah Connor", 200L)));
        customers.add(createCustomer(300L, "James Patel", "9 Unter den Linden, Berlin, Germany", 3L, TrustLevel.L1, buildFakeCustomerEmail("James Patel", 300L)));
        customers.add(createCustomer(400L, "Olivia Murphy", "31 Gran Via, Madrid, Spain", 4L, TrustLevel.L2, buildFakeCustomerEmail("Olivia Murphy", 400L)));
        customers.add(createCustomer(500L, "Liam O'Connor", "8 Via Roma, Milan, Italy", 5L, TrustLevel.L3, buildFakeCustomerEmail("Liam O'Connor", 500L)));
        customers.add(createCustomer(600L, "Ava Thompson", "56 Damrak, Amsterdam, Netherlands", 1L, TrustLevel.L1, buildFakeCustomerEmail("Ava Thompson", 600L)));

        for (int i = 0; i < 100; i++) {
            String first = GLOBAL_FIRST_NAMES[i % GLOBAL_FIRST_NAMES.length];
            String last = GLOBAL_LAST_NAMES[(i * 5) % GLOBAL_LAST_NAMES.length];
            long id = 2000L + i;
            long employeeId = employees.get(i % employees.size()).getEmployeeId();
            CityCountry location = GLOBAL_LOCATIONS[i % GLOBAL_LOCATIONS.length];
            String address = buildDiverseAddress(100 + i, i, location);
            TrustLevel trustLevel = switch (i % 3) {
                case 0 -> TrustLevel.L1;
                case 1 -> TrustLevel.L2;
                default -> TrustLevel.L3;
            };
            String fullName = first + " " + last;
            customers.add(createCustomer(id, fullName, address, employeeId, trustLevel, buildFakeCustomerEmail(fullName, id)));
        }

        return customerRepo.saveAll(customers);
    }

    private void seedExternalCompanies(ExternalCompanyRepository companyRepo) {
        List<ExternalCompany> companies = new ArrayList<>();

        companies.add(createCompany(1000L, "Globex Inc", "1 Main St, Metropolis", "Jane Doe", "555-111-2222", false));
        companies.add(createCompany(1500L, "Fustion Technologies", "88 Innovation Park, Dublin", "Alex Murphy", "555-444-1212", false));
        companies.add(createCompany(2000L, "Initech", "42 Silicon Ave, Tech City", "John Roe", "555-333-4444", false));
        companies.add(createCompany(3000L, "Umbrella Financial", "17 Queen Rd, Dublin", "Martha Lane", "555-222-8888", true));

        for (int i = 0; i < 100; i++) {
            CityCountry location = GLOBAL_LOCATIONS[(i * 2) % GLOBAL_LOCATIONS.length];
            long id = 4000L + i;
            String lead = COMPANY_NAME_PREFIXES[i % COMPANY_NAME_PREFIXES.length];
            String middle = COMPANY_NAME_MIDDLES[(i * 3) % COMPANY_NAME_MIDDLES.length];
            String end = COMPANY_NAME_SUFFIXES[(i * 5) % COMPANY_NAME_SUFFIXES.length];
            String name = lead + " " + middle + " " + end;
            String address = buildDiverseAddress(10 + i, i + 31, location);
            String contact = CONTACT_FIRST_NAMES[i % CONTACT_FIRST_NAMES.length] + " "
                + CONTACT_LAST_NAMES[(i * 7) % CONTACT_LAST_NAMES.length];
            String phone = buildRegionalPhone(location.country(), i);
            boolean sanctioned = (i % 7 == 0);
            companies.add(createCompany(id, name, address, contact, phone, sanctioned));
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

    private Customer createCustomer(Long id,
                                    String name,
                                    String address,
                                    Long employeeId,
                                    TrustLevel trustLevel,
                                    String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setCustomerName(name);
        customer.setAddress(address);
        customer.setEmployeeId(employeeId);
        customer.setTrustLevel(trustLevel);
        customer.setEmail(email);
        return customer;
    }

    private void backfillCustomerTrustLevels(CustomerRepository customerRepo) {
        List<Customer> customers = customerRepo.findAll();
        List<Customer> updated = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.getTrustLevel() == null) {
                customer.setTrustLevel(TrustLevel.L2);
                updated.add(customer);
            }
        }
        if (!updated.isEmpty()) {
            customerRepo.saveAll(updated);
        }
    }

    private void backfillCustomerEmails(CustomerRepository customerRepo) {
        List<Customer> customers = customerRepo.findAll();
        List<Customer> updated = new ArrayList<>();
        for (Customer customer : customers) {
            String currentEmail = customer.getEmail();
            boolean emailMissing = currentEmail == null || currentEmail.trim().isEmpty();
            boolean isNiall = customer.getCustomerName() != null && NIAL_DEEHAN_NAME.equalsIgnoreCase(customer.getCustomerName().trim());

            if (isNiall && !NIAL_DEEHAN_EMAIL.equalsIgnoreCase(currentEmail)) {
                customer.setEmail(NIAL_DEEHAN_EMAIL);
                updated.add(customer);
                continue;
            }

            if (emailMissing) {
                customer.setEmail(buildFakeCustomerEmail(customer.getCustomerName(), customer.getCustomerId()));
                updated.add(customer);
            }
        }

        if (!updated.isEmpty()) {
            customerRepo.saveAll(updated);
        }
    }

    private String buildFakeCustomerEmail(String fullName, Long customerId) {
        String safeName = fullName == null ? "customer" : fullName.toLowerCase()
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("(^\\.+|\\.+$)", "");
        if (safeName.isBlank()) {
            safeName = "customer";
        }
        long safeId = customerId == null ? 0L : customerId;
        return safeName + "." + safeId + "@example.test";
    }

    private String buildDiverseAddress(int houseNumber, int index, CityCountry location) {
        String street = GLOBAL_STREETS[index % GLOBAL_STREETS.length];
        String postalChunk = String.format("%03d", (index * 17) % 1000);
        return switch (index % 4) {
            case 0 -> houseNumber + " " + street + ", " + location.city() + ", " + location.country();
            case 1 -> houseNumber + " " + street + ", " + location.city() + " " + postalChunk + ", " + location.country();
            case 2 -> "Unit " + ((index % 12) + 1) + ", " + houseNumber + " " + street + ", " + location.city() + ", " + location.country();
            default -> houseNumber + " " + street + " District, " + location.city() + ", " + location.country();
        };
    }

    private String buildRegionalPhone(String country, int index) {
        String dialCode = switch (country) {
            case "Ireland" -> "353";
            case "Nigeria" -> "234";
            case "India" -> "91";
            case "Brazil" -> "55";
            case "Japan" -> "81";
            case "South Korea" -> "82";
            case "Mexico" -> "52";
            case "UAE" -> "971";
            case "Kenya" -> "254";
            case "Germany" -> "49";
            case "Denmark" -> "45";
            case "New Zealand" -> "64";
            case "Turkiye" -> "90";
            case "South Africa" -> "27";
            case "Chile" -> "56";
            default -> "44";
        };
        return String.format("+%s-%03d-%04d", dialCode, (index % 900) + 100, (index * 29 % 9000) + 1000);
    }

    private record CityCountry(String city, String country) {
    }

    private void seedInsurancePolicies(InsurancePolicyRepository policyRepo,
                                        List<Customer> customers,
                                        List<ExternalCompany> companies) {
        List<InsurancePolicy> policies = new ArrayList<>();

        // --- Hardcoded anchor policies ---
        policies.add(createCarPolicy(1L, customers.get(0).getCustomerId(), LocalDate.of(2023, 3, 1), LocalDate.of(2024, 2, 29), "195.00", "15000.00", "162-ND-2023", "Toyota", "Corolla", 2019));
        policies.add(createHomePolicy(2L, customers.get(1).getCustomerId(), LocalDate.of(2022, 6, 15), LocalDate.of(2025, 6, 14), "420.00", "350000.00", "77 River Lane, Cork, Ireland", "House", "350000.00"));
        policies.add(createPetPolicy(3L, customers.get(2).getCustomerId(), LocalDate.of(2024, 1, 10), null, "55.00", "5000.00", "Max", "Dog", "Labrador", 4));
        policies.add(createLtiPolicy(4L, customers.get(3).getCustomerId(), LocalDate.of(2021, 9, 1), LocalDate.of(2031, 8, 31), "310.00", "250000.00", "2500.00", 90, 60));
        // Company-held policies
        policies.add(createCarPolicy(5L, null, LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30), "650.00", "45000.00", "151-GX-2022", "Mercedes", "Sprinter", 2022));
        policies.get(4).setHolderType(PolicyHolderType.EXTERNAL_COMPANY);
        policies.get(4).setCompanyId(companies.get(0).getCompanyId());
        policies.get(4).setCustomerId(null);

        policies.add(createHomePolicy(6L, null, LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31), "980.00", "1200000.00", "1 Main St, Metropolis", "Commercial", "1200000.00"));
        policies.get(5).setHolderType(PolicyHolderType.EXTERNAL_COMPANY);
        policies.get(5).setCompanyId(companies.get(1).getCompanyId());
        policies.get(5).setCustomerId(null);

        // --- Generated policies ---
        PolicyType[] types = PolicyType.values();
        PolicyStatus[] statuses = { PolicyStatus.ACTIVE, PolicyStatus.ACTIVE, PolicyStatus.ACTIVE, PolicyStatus.EXPIRED, PolicyStatus.CANCELLED };
        String[] carMakes  = { "Toyota", "Ford", "BMW", "Honda", "Volkswagen", "Renault", "Peugeot", "Hyundai", "Nissan", "Audi" };
        String[] carModels = { "Corolla", "Focus", "3 Series", "Civic", "Golf", "Clio", "208", "i30", "Qashqai", "A4" };
        String[] petNames    = { "Buddy", "Luna", "Max", "Bella", "Charlie", "Lucy", "Cooper", "Daisy", "Rocky", "Molly" };
        String[] petSpecies  = { "Dog", "Cat", "Dog", "Cat", "Dog", "Cat", "Dog", "Dog", "Dog", "Cat" };
        String[] petBreeds   = { "Labrador", "Persian", "Beagle", "Siamese", "Spaniel", "Maine Coon", "Poodle", "Golden Retriever", "Boxer", "Ragdoll" };
        String[] propTypes   = { "House", "Apartment", "Bungalow", "Duplex", "Studio" };

        for (int i = 0; i < 44; i++) {
            long id = 100L + i;
            PolicyType type = types[i % types.length];
            PolicyStatus status = statuses[i % statuses.length];
            boolean useCompany = i % 5 == 4;

            LocalDate start = LocalDate.of(2020 + (i % 5), (i % 12) + 1, (i % 27) + 1);
            LocalDate end   = i % 7 == 0 ? null : start.plusYears(1);

            String premium  = String.valueOf(80 + (i * 17) % 900);
            String coverage = String.valueOf(10000 + (i * 3750) % 500000);

            InsurancePolicy policy = switch (type) {
                case CAR -> createCarPolicy(id, useCompany ? null : customers.get(i % customers.size()).getCustomerId(),
                        start, end, premium, coverage,
                        String.format("%03d-XX-%d", (i % 900) + 100, 2015 + (i % 10)),
                        carMakes[i % carMakes.length], carModels[i % carModels.length],
                        2015 + (i % 10));
                case HOME -> createHomePolicy(id, useCompany ? null : customers.get(i % customers.size()).getCustomerId(),
                        start, end, premium, coverage,
                        buildDiverseAddress(10 + i, i, GLOBAL_LOCATIONS[i % GLOBAL_LOCATIONS.length]),
                        propTypes[i % propTypes.length],
                        String.valueOf(150000 + (i * 12500) % 800000));
                case PET -> createPetPolicy(id, useCompany ? null : customers.get(i % customers.size()).getCustomerId(),
                        start, end, premium, coverage,
                        petNames[i % petNames.length], petSpecies[i % petSpecies.length],
                        petBreeds[i % petBreeds.length], 1 + (i % 14));
                case LONG_TERM_INJURY -> createLtiPolicy(id,
                        useCompany ? null : customers.get(i % customers.size()).getCustomerId(),
                        start, end, premium, coverage,
                        String.valueOf(1000 + (i * 250) % 4000), 30 + (i % 4) * 30, 24 + (i % 4) * 12);
            };

            policy.setStatus(status);

            if (useCompany) {
                policy.setHolderType(PolicyHolderType.EXTERNAL_COMPANY);
                policy.setCompanyId(companies.get(i % companies.size()).getCompanyId());
                policy.setCustomerId(null);
            }

            policies.add(policy);
        }

        policyRepo.saveAll(policies);
    }

    private void seedPackages(PackageRepository packageRepo, List<Customer> customers) {
        List<Package> packages = new ArrayList<>();

        // --- Hardcoded anchor packages ---
        packages.add(createPackage(1L, customers.get(0).getCustomerId(), PackageStatus.DELIVERED, ServiceLevel.EXPRESS,
                "Niall Deehan", "Aoife Byrne",
                "14 Fitzwilliam Square, Dublin, Ireland", "8 Patrick Street, Cork, Ireland",
                "2.450", LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 3),
                "12.50", "Books and stationery"));
        packages.add(createPackage(2L, customers.get(1).getCustomerId(), PackageStatus.IN_TRANSIT, ServiceLevel.STANDARD,
                "John Doe", "Marta Rossi",
                "77 River Lane, Cork, Ireland", "22 Via Roma, Milan, Italy",
                "5.200", LocalDate.of(2026, 5, 28), LocalDate.of(2026, 6, 4), null,
                "28.90", "Spare machine parts"));
        packages.add(createPackage(3L, customers.get(2).getCustomerId(), PackageStatus.CREATED, ServiceLevel.OVERNIGHT,
                "Sarah Connor", "Tomas Novak",
                "22 Rue Saint-Honore, Paris, France", "9 Unter den Linden, Berlin, Germany",
                "0.800", null, LocalDate.of(2026, 6, 2), null,
                "45.00", "Legal documents"));

        // --- Generated packages ---
        PackageStatus[] statuses = PackageStatus.values();
        ServiceLevel[] levels = ServiceLevel.values();
        String[] contents = { "Electronics", "Clothing", "Books", "Food items", "Machine parts",
                "Cosmetics", "Toys", "Documents", "Homeware", "Medical supplies" };
        java.util.Random random = new java.util.Random(42);

        for (int i = 0; i < 60; i++) {
            long id = 100L + i;
            PackageStatus status = statuses[i % statuses.length];
            ServiceLevel level = levels[i % levels.length];
            Customer sender = customers.get(i % customers.size());
            Customer recipient = customers.get((i * 7 + 3) % customers.size());

            LocalDate shipped = status == PackageStatus.CREATED ? null
                    : LocalDate.of(2026, (i % 5) + 1, (i % 27) + 1);
            LocalDate estimated = shipped == null ? LocalDate.of(2026, 6, (i % 27) + 1)
                    : shipped.plusDays(2 + (i % 6));
            LocalDate delivered = (status == PackageStatus.DELIVERED && estimated != null)
                    ? estimated.minusDays(i % 2) : null;

            String weight = String.format(java.util.Locale.ROOT, "%.3f", 0.2 + (random.nextDouble() * 24.8));
            String cost = String.format(java.util.Locale.ROOT, "%.2f", 5.0 + (random.nextDouble() * 95.0));

            packages.add(createPackage(id, sender.getCustomerId(), status, level,
                    sender.getCustomerName(), recipient.getCustomerName(),
                    sender.getAddress(), recipient.getAddress(),
                    weight, shipped, estimated, delivered,
                    cost, contents[i % contents.length]));
        }

        packageRepo.saveAll(packages);
    }

    private Package createPackage(Long id, Long customerId, PackageStatus status, ServiceLevel serviceLevel,
                                  String senderName, String recipientName,
                                  String originAddress, String destinationAddress,
                                  String weightKg, LocalDate shippedDate, LocalDate estimatedDeliveryDate,
                                  LocalDate deliveredDate, String shippingCost, String contents) {
        Package p = new Package();
        p.setPackageId(id);
        p.setTrackingNumber(String.format("PKG-%06d", id));
        p.setStatus(status);
        p.setServiceLevel(serviceLevel);
        p.setSenderName(senderName);
        p.setRecipientName(recipientName);
        p.setOriginAddress(originAddress);
        p.setDestinationAddress(destinationAddress);
        p.setWeightKg(new BigDecimal(weightKg));
        p.setCustomerId(customerId);
        p.setShippedDate(shippedDate);
        p.setEstimatedDeliveryDate(estimatedDeliveryDate);
        p.setDeliveredDate(deliveredDate);
        p.setShippingCost(shippingCost != null ? new BigDecimal(shippingCost) : null);
        p.setContents(contents);
        return p;
    }

    private List<Product> seedProducts(ProductRepository productRepo) {
        List<Product> products = new ArrayList<>();

        // --- Real consumer-electronics catalogue (static list) ---
        // Phones
        products.add(createProduct(1L,  "APL-IPH15PM", "Apple iPhone 15 Pro Max",          "6.7-inch titanium iPhone with the A17 Pro chip, 5x telephoto camera and USB-C.",        "Phones", "Apple",     "1199.00", "743.00", true,  138, "premium"));
        products.add(createProduct(2L,  "APL-IPH15P",  "Apple iPhone 15 Pro",              "6.1-inch titanium iPhone with the A17 Pro chip, Action button and USB-C.",              "Phones", "Apple",     "999.00",  "619.00", true,  204, "premium"));
        products.add(createProduct(3L,  "APL-IPH15PL", "Apple iPhone 15 Plus",             "6.7-inch iPhone with the A16 Bionic chip, 48MP main camera and Dynamic Island.",        "Phones", "Apple",     "899.00",  "557.00", true,  96,  "standard"));
        products.add(createProduct(4L,  "APL-IPH15",   "Apple iPhone 15",                  "6.1-inch iPhone with the A16 Bionic chip, 48MP main camera and USB-C.",                 "Phones", "Apple",     "799.00",  "495.00", true,  260, "standard"));
        products.add(createProduct(5L,  "APL-IPH14",   "Apple iPhone 14",                  "6.1-inch iPhone with the A15 Bionic chip and advanced dual-camera system.",             "Phones", "Apple",     "699.00",  "433.00", false, 0,   "standard"));
        products.add(createProduct(6L,  "APL-IPHSE3",  "Apple iPhone SE (3rd gen)",        "Compact 4.7-inch iPhone with the A15 Bionic chip and Touch ID.",                        "Phones", "Apple",     "429.00",  "266.00", true,  150, "budget"));
        products.add(createProduct(7L,  "SAM-S24U",    "Samsung Galaxy S24 Ultra",         "6.8-inch QHD+ flagship with the Snapdragon 8 Gen 3, 200MP camera and S Pen.",           "Phones", "Samsung",   "1299.00", "805.00", true,  112, "premium"));
        products.add(createProduct(8L,  "SAM-S24P",    "Samsung Galaxy S24+",              "6.7-inch QHD+ smartphone with Galaxy AI features and a 4900mAh battery.",               "Phones", "Samsung",   "999.00",  "619.00", true,  88,  "premium"));
        products.add(createProduct(9L,  "SAM-S24",     "Samsung Galaxy S24",               "6.2-inch flagship smartphone with Galaxy AI and a triple rear camera.",                 "Phones", "Samsung",   "799.00",  "495.00", true,  176, "standard"));
        products.add(createProduct(10L, "SAM-ZFOLD5",  "Samsung Galaxy Z Fold5",           "Foldable 7.6-inch main display with a 6.2-inch cover screen and S Pen support.",        "Phones", "Samsung",   "1799.00", "1115.00", true, 24,  "premium"));
        products.add(createProduct(11L, "SAM-ZFLIP5",  "Samsung Galaxy Z Flip5",           "Compact flip phone with a 6.7-inch main screen and 3.4-inch Flex Window.",              "Phones", "Samsung",   "999.00",  "619.00", true,  60,  "premium"));
        products.add(createProduct(12L, "SAM-A54",     "Samsung Galaxy A54",               "Mid-range smartphone with a 6.4-inch Super AMOLED display and 50MP camera.",            "Phones", "Samsung",   "449.00",  "278.00", true,  320, "budget"));
        products.add(createProduct(13L, "GGL-PX8P",    "Google Pixel 8 Pro",               "6.7-inch LTPO OLED phone with the Tensor G3 chip and pro-grade triple camera.",         "Phones", "Google",    "999.00",  "619.00", true,  94,  "premium"));
        products.add(createProduct(14L, "GGL-PX8",     "Google Pixel 8",                   "6.2-inch Actua display phone with the Tensor G3 chip and AI photo tools.",              "Phones", "Google",    "699.00",  "433.00", true,  148, "standard"));
        products.add(createProduct(15L, "GGL-PX7A",    "Google Pixel 7a",                  "Compact 6.1-inch phone with the Tensor G2 chip and dual rear camera.",                  "Phones", "Google",    "499.00",  "309.00", true,  212, "budget"));
        products.add(createProduct(16L, "ONP-12",      "OnePlus 12",                       "6.82-inch QHD+ flagship with the Snapdragon 8 Gen 3 and 100W fast charging.",           "Phones", "OnePlus",   "799.00",  "495.00", true,  72,  "premium"));
        products.add(createProduct(17L, "ONP-11",      "OnePlus 11",                       "6.7-inch AMOLED phone with the Snapdragon 8 Gen 2 and Hasselblad cameras.",             "Phones", "OnePlus",   "699.00",  "433.00", true,  90,  "standard"));
        products.add(createProduct(18L, "XMI-14",      "Xiaomi 14",                        "6.36-inch flagship with the Snapdragon 8 Gen 3 and Leica-tuned cameras.",               "Phones", "Xiaomi",    "899.00",  "557.00", true,  64,  "premium"));
        products.add(createProduct(19L, "XMI-RN13P",   "Xiaomi Redmi Note 13 Pro",         "6.67-inch AMOLED phone with a 200MP camera and 67W charging.",                          "Phones", "Xiaomi",    "379.00",  "235.00", true,  280, "budget"));
        products.add(createProduct(20L, "MOT-EDGE40",  "Motorola Edge 40",                 "6.55-inch pOLED phone with a curved display and 144Hz refresh rate.",                   "Phones", "Motorola",  "599.00",  "371.00", true,  130, "standard"));
        products.add(createProduct(21L, "NTH-PHONE2",  "Nothing Phone (2)",                "6.7-inch OLED phone with the Glyph interface and Snapdragon 8+ Gen 1.",                 "Phones", "Nothing",   "599.00",  "371.00", true,  85,  "standard"));
        products.add(createProduct(22L, "SNY-XP1V",    "Sony Xperia 1 V",                  "6.5-inch 4K OLED phone with a pro camera system and 120Hz display.",                    "Phones", "Sony",      "1399.00", "867.00", true,  38,  "premium"));

        // Laptops
        products.add(createProduct(23L, "APL-MBP16M3X", "Apple MacBook Pro 16\" (M3 Max)", "16-inch Liquid Retina XDR laptop with the M3 Max chip for demanding workloads.",        "Laptops", "Apple",     "3499.00", "2169.00", false, 0,  "premium"));
        products.add(createProduct(24L, "APL-MBP14M3",  "Apple MacBook Pro 14\" (M3)",     "14-inch Liquid Retina XDR laptop with the M3 chip and all-day battery.",                "Laptops", "Apple",     "1599.00", "991.00",  true,  72, "premium"));
        products.add(createProduct(25L, "APL-MBA15M2",  "Apple MacBook Air 15\" (M2)",     "15-inch laptop with the M2 chip, fanless design and Liquid Retina display.",            "Laptops", "Apple",     "1299.00", "805.00",  true,  118, "premium"));
        products.add(createProduct(26L, "APL-MBA13M2",  "Apple MacBook Air 13\" (M2)",     "13-inch ultralight laptop with the M2 chip and 18-hour battery life.",                  "Laptops", "Apple",     "1099.00", "681.00",  true,  204, "standard"));
        products.add(createProduct(27L, "DEL-XPS15",   "Dell XPS 15",                      "15.6-inch laptop with a 13th-gen Intel Core i7 and InfinityEdge display.",              "Laptops", "Dell",      "1499.00", "929.00",  true,  56,  "premium"));
        products.add(createProduct(28L, "DEL-XPS13",   "Dell XPS 13",                      "13.4-inch compact laptop with a 13th-gen Intel Core i5 and CNC aluminium build.",       "Laptops", "Dell",      "999.00",  "619.00",  true,  84,  "standard"));
        products.add(createProduct(29L, "DEL-INS15",   "Dell Inspiron 15",                 "15.6-inch everyday laptop with a Full HD display and Intel Core i5.",                   "Laptops", "Dell",      "649.00",  "402.00",  true,  240, "budget"));
        products.add(createProduct(30L, "HP-SPCX360",  "HP Spectre x360 14",               "14-inch 2-in-1 convertible with an OLED touchscreen and Intel Evo platform.",           "Laptops", "HP",        "1449.00", "898.00",  true,  48,  "premium"));
        products.add(createProduct(31L, "HP-PAV15",    "HP Pavilion 15",                   "15.6-inch laptop with an AMD Ryzen 5 processor for work and study.",                    "Laptops", "HP",        "799.00",  "495.00",  true,  190, "budget"));
        products.add(createProduct(32L, "LEN-X1C11",   "Lenovo ThinkPad X1 Carbon Gen 11", "14-inch business ultrabook with a carbon-fibre chassis and Intel vPro.",                "Laptops", "Lenovo",    "1699.00", "1053.00", true,  62,  "premium"));
        products.add(createProduct(33L, "LEN-LGNPRO7", "Lenovo Legion Pro 7",              "16-inch gaming laptop with an RTX 4080 GPU and 240Hz display.",                         "Laptops", "Lenovo",    "2299.00", "1425.00", true,  30,  "premium"));
        products.add(createProduct(34L, "LEN-YOGA9I",  "Lenovo Yoga 9i",                   "14-inch 2-in-1 with a 4K OLED touchscreen and rotating soundbar hinge.",                "Laptops", "Lenovo",    "1399.00", "867.00",  true,  58,  "premium"));
        products.add(createProduct(35L, "ASU-ROGG14",  "ASUS ROG Zephyrus G14",            "14-inch gaming laptop with a Ryzen 9 CPU and RTX 4060 in a compact body.",              "Laptops", "ASUS",      "1599.00", "991.00",  true,  44,  "premium"));
        products.add(createProduct(36L, "ASU-ZEN14",   "ASUS ZenBook 14",                  "14-inch OLED ultrabook with an Intel Core i7 and lightweight design.",                  "Laptops", "ASUS",      "999.00",  "619.00",  true,  96,  "standard"));
        products.add(createProduct(37L, "MSF-SLP5",    "Microsoft Surface Laptop 5",       "13.5-inch touchscreen laptop with a 12th-gen Intel Core i5 and PixelSense display.",    "Laptops", "Microsoft", "1299.00", "805.00",  true,  70,  "standard"));
        products.add(createProduct(38L, "ACR-SWIFT3",  "Acer Swift 3",                     "14-inch thin-and-light laptop with a Full HD display and Intel Core i5.",               "Laptops", "Acer",      "699.00",  "433.00",  true,  165, "budget"));
        products.add(createProduct(39L, "ACR-PRH16",   "Acer Predator Helios 16",          "16-inch gaming laptop with a 13th-gen Core i7 and RTX 4070 GPU.",                       "Laptops", "Acer",      "1699.00", "1053.00", true,  36,  "premium"));
        products.add(createProduct(40L, "RZR-BLD15",   "Razer Blade 15",                   "15.6-inch gaming laptop with a QHD 240Hz display and RTX 4070 GPU.",                    "Laptops", "Razer",     "2499.00", "1549.00", false, 0,   "premium"));

        // Tablets
        products.add(createProduct(41L, "APL-IPDP129", "Apple iPad Pro 12.9\" (M2)",       "12.9-inch Liquid Retina XDR tablet with the M2 chip and Apple Pencil support.",         "Tablets", "Apple",     "1099.00", "681.00", true,  92,  "premium"));
        products.add(createProduct(42L, "APL-IPDAIR5", "Apple iPad Air (5th gen)",         "10.9-inch tablet with the M1 chip and a Liquid Retina display.",                        "Tablets", "Apple",     "599.00",  "371.00", true,  186, "standard"));
        products.add(createProduct(43L, "APL-IPD10",   "Apple iPad (10th gen)",            "10.9-inch tablet with the A14 Bionic chip and USB-C.",                                  "Tablets", "Apple",     "449.00",  "278.00", true,  310, "budget"));
        products.add(createProduct(44L, "APL-IPDMINI6","Apple iPad mini (6th gen)",        "8.3-inch compact tablet with the A15 Bionic chip and Apple Pencil support.",            "Tablets", "Apple",     "499.00",  "309.00", true,  140, "standard"));
        products.add(createProduct(45L, "SAM-TABS9U",  "Samsung Galaxy Tab S9 Ultra",      "14.6-inch AMOLED tablet with the Snapdragon 8 Gen 2 and S Pen.",                        "Tablets", "Samsung",   "1199.00", "743.00", true,  40,  "premium"));
        products.add(createProduct(46L, "SAM-TABS9",   "Samsung Galaxy Tab S9",            "11-inch AMOLED tablet with an IP68 rating and bundled S Pen.",                          "Tablets", "Samsung",   "799.00",  "495.00", true,  98,  "standard"));
        products.add(createProduct(47L, "SAM-TABA9P",  "Samsung Galaxy Tab A9+",           "11-inch entry tablet with a 90Hz display and quad speakers.",                          "Tablets", "Samsung",   "219.00",  "136.00", true,  350, "budget"));
        products.add(createProduct(48L, "MSF-SP9",     "Microsoft Surface Pro 9",          "13-inch 2-in-1 tablet with a 12th-gen Intel Core i5 and detachable keyboard.",          "Tablets", "Microsoft", "999.00",  "619.00", true,  66,  "premium"));
        products.add(createProduct(49L, "LEN-TABP12",  "Lenovo Tab P12",                   "12.7-inch Android tablet with a 3K display and Tab Pen included.",                      "Tablets", "Lenovo",    "349.00",  "216.00", true,  175, "budget"));
        products.add(createProduct(50L, "AMZ-FMAX11",  "Amazon Fire Max 11",               "11-inch tablet with an octa-core processor and aluminium body.",                        "Tablets", "Amazon",    "229.00",  "142.00", true,  410, "budget"));
        products.add(createProduct(51L, "GGL-PXTAB",   "Google Pixel Tablet",              "11-inch Android tablet with the Tensor G2 chip and charging speaker dock.",             "Tablets", "Google",    "499.00",  "309.00", true,  120, "standard"));

        // Watches
        products.add(createProduct(52L, "APL-AWU2",    "Apple Watch Ultra 2",              "49mm titanium smartwatch with a brighter display, GPS and 36-hour battery.",            "Watches", "Apple",     "799.00",  "495.00", true,  110, "premium"));
        products.add(createProduct(53L, "APL-AWS9",    "Apple Watch Series 9",             "45mm smartwatch with the S9 chip, double-tap gesture and always-on display.",           "Watches", "Apple",     "399.00",  "247.00", true,  280, "standard"));
        products.add(createProduct(54L, "APL-AWSE2",   "Apple Watch SE (2nd gen)",         "40mm smartwatch with crash detection and fitness tracking.",                            "Watches", "Apple",     "249.00",  "154.00", true,  340, "budget"));
        products.add(createProduct(55L, "SAM-GW6C",    "Samsung Galaxy Watch6 Classic",    "47mm smartwatch with a rotating bezel and advanced health sensors.",                    "Watches", "Samsung",   "399.00",  "247.00", true,  150, "standard"));
        products.add(createProduct(56L, "SAM-GW6",     "Samsung Galaxy Watch6",            "44mm smartwatch with body composition analysis and sleep coaching.",                    "Watches", "Samsung",   "299.00",  "185.00", true,  220, "standard"));
        products.add(createProduct(57L, "GGL-PXW2",    "Google Pixel Watch 2",             "41mm smartwatch with Fitbit health tracking and the Wear OS platform.",                 "Watches", "Google",    "349.00",  "216.00", true,  160, "standard"));
        products.add(createProduct(58L, "GMN-FENIX7",  "Garmin Fenix 7",                   "Multisport GPS watch with solar charging and rugged construction.",                     "Watches", "Garmin",    "699.00",  "433.00", true,  74,  "premium"));
        products.add(createProduct(59L, "GMN-VENU3",   "Garmin Venu 3",                    "AMOLED GPS smartwatch with detailed health and sleep tracking.",                        "Watches", "Garmin",    "449.00",  "278.00", true,  130, "standard"));
        products.add(createProduct(60L, "GMN-FR265",   "Garmin Forerunner 265",            "AMOLED running watch with training metrics and multi-band GPS.",                        "Watches", "Garmin",    "449.00",  "278.00", true,  118, "standard"));
        products.add(createProduct(61L, "FTB-SENSE2",  "Fitbit Sense 2",                   "Health smartwatch with stress, ECG and skin temperature sensors.",                      "Watches", "Fitbit",    "299.00",  "185.00", true,  200, "budget"));
        products.add(createProduct(62L, "FTB-VERSA4",  "Fitbit Versa 4",                   "Fitness smartwatch with built-in GPS and 6-day battery life.",                          "Watches", "Fitbit",    "199.00",  "123.00", true,  290, "budget"));
        products.add(createProduct(63L, "WTH-SCANW",   "Withings ScanWatch",               "Hybrid smartwatch with ECG, SpO2 and an analogue design.",                              "Watches", "Withings",  "279.00",  "173.00", true,  90,  "standard"));
        products.add(createProduct(64L, "AMZ-GTR4",    "Amazfit GTR 4",                    "AMOLED smartwatch with dual-band GPS and 14-day battery life.",                         "Watches", "Amazfit",   "199.00",  "123.00", true,  175, "budget"));

        return productRepo.saveAll(products);
    }

    private Product createProduct(Long id, String sku, String name, String description, String category, String manufacturer,
                                  String rrp, String unitCost, boolean inStock, int stockQty, String tier) {
        Product p = new Product();
        p.setProductId(id);
        p.setSku(sku);
        p.setName(name);
        p.setDescription(description);
        p.setCategory(category);
        p.setManufacturer(manufacturer);
        p.setRrp(new BigDecimal(rrp));
        p.setUnitCost(new BigDecimal(unitCost));
        p.setInStock(inStock);
        p.setStockQty(stockQty);
        p.setTier(tier);
        return p;
    }

    private void seedPurchaseOrders(PurchaseOrderRepository orderRepo, List<Product> products, List<Customer> customers) {
        if (products.isEmpty()) {
            return;
        }
        List<PurchaseOrder> orders = new ArrayList<>();
        OrderStatus[] statuses = OrderStatus.values();
        java.util.Random random = new java.util.Random(13);

        for (int i = 0; i < 30; i++) {
            long id = 1L + i;
            PurchaseOrder order = new PurchaseOrder();
            order.setOrderId(id);
            order.setOrderNumber(String.format("PO-%06d", id));
            order.setStatus(statuses[i % statuses.length]);
            order.setCustomerId(customers.get(i % customers.size()).getCustomerId());
            LocalDate orderDate = LocalDate.of(2026, (i % 5) + 1, (i % 27) + 1);
            order.setOrderDate(orderDate);
            order.setExpectedDeliveryDate(orderDate.plusDays(3 + (i % 10)));
            order.setNotes(i % 4 == 0 ? "Priority restock order" : null);

            int itemCount = 1 + (i % 3);
            for (int l = 0; l < itemCount; l++) {
                Product product = products.get((i * 3 + l) % products.size());
                int qty = 1 + random.nextInt(20);
                PurchaseItem item = new PurchaseItem();
                item.setProductId(product.getProductId());
                item.setProductName(product.getName());
                item.setQuantity(qty);
                item.setUnitPrice(product.getRrp());
                item.setOrder(order);
                order.getItems().add(item);
            }

            BigDecimal total = BigDecimal.ZERO;
            for (PurchaseItem item : order.getItems()) {
                total = total.add(item.getLineTotal());
            }
            order.setTotalAmount(total);

            orders.add(order);
        }

        orderRepo.saveAll(orders);
    }

    private InsurancePolicy createCarPolicy(Long id, Long customerId,
                                             LocalDate start, LocalDate end,
                                             String premium, String coverage,
                                             String reg, String make, String model, int year) {
        InsurancePolicy p = basePolicy(id, customerId, PolicyType.CAR, start, end, premium, coverage);
        p.setVehicleRegistration(reg);
        p.setVehicleMake(make);
        p.setVehicleModel(model);
        p.setVehicleYear(year);
        return p;
    }

    private InsurancePolicy createHomePolicy(Long id, Long customerId,
                                              LocalDate start, LocalDate end,
                                              String premium, String coverage,
                                              String address, String propertyType, String value) {
        InsurancePolicy p = basePolicy(id, customerId, PolicyType.HOME, start, end, premium, coverage);
        p.setPropertyAddress(address);
        p.setPropertyType(propertyType);
        p.setPropertyValue(new java.math.BigDecimal(value));
        return p;
    }

    private InsurancePolicy createPetPolicy(Long id, Long customerId,
                                             LocalDate start, LocalDate end,
                                             String premium, String coverage,
                                             String name, String species, String breed, int age) {
        InsurancePolicy p = basePolicy(id, customerId, PolicyType.PET, start, end, premium, coverage);
        p.setPetName(name);
        p.setPetSpecies(species);
        p.setPetBreed(breed);
        p.setPetAge(age);
        return p;
    }

    private InsurancePolicy createLtiPolicy(Long id, Long customerId,
                                             LocalDate start, LocalDate end,
                                             String premium, String coverage,
                                             String monthlyBenefit, int waitDays, int maxMonths) {
        InsurancePolicy p = basePolicy(id, customerId, PolicyType.LONG_TERM_INJURY, start, end, premium, coverage);
        p.setMonthlyBenefit(new java.math.BigDecimal(monthlyBenefit));
        p.setWaitingPeriodDays(waitDays);
        p.setMaxBenefitPeriodMonths(maxMonths);
        return p;
    }

    private InsurancePolicy basePolicy(Long id, Long customerId, PolicyType type,
                                        LocalDate start, LocalDate end,
                                        String premium, String coverage) {
        InsurancePolicy p = new InsurancePolicy();
        p.setPolicyId(id);
        p.setPolicyNumber(String.format("POL-%06d", id));
        p.setPolicyType(type);
        p.setStatus(PolicyStatus.ACTIVE);
        p.setHolderType(PolicyHolderType.CUSTOMER);
        p.setCustomerId(customerId);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setPremiumAmount(new java.math.BigDecimal(premium));
        p.setCoverageAmount(new java.math.BigDecimal(coverage));
        return p;
    }

    private ExternalCompany createCompany(Long id, String name, String address, String contactPerson, String phone, boolean sanctioned) {
        ExternalCompany company = new ExternalCompany();
        company.setCompanyId(id);
        company.setCompanyName(name);
        company.setAddress(address);
        company.setContactPerson(contactPerson);
        company.setPhoneNumber(phone);
        company.setSanctioned(sanctioned);
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
