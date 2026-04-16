package com.example.camunda;

import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.AccountType;
import com.example.camunda.model.Customer;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.model.InsurancePolicy;
import com.example.camunda.model.PolicyHolderType;
import com.example.camunda.model.PolicyStatus;
import com.example.camunda.model.PolicyType;
import com.example.camunda.model.TrustLevel;
import com.example.camunda.repository.AccountRepository;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.CustomerAccountRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.repository.ExternalCompanyRepository;
import com.example.camunda.repository.InsurancePolicyRepository;
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
                               @Value("${app.seed.reset-on-startup:false}") boolean resetOnStartup) {
        return args -> {
            backfillCustomerTrustLevels(customerRepo);
            backfillCustomerEmails(customerRepo);

            if (resetOnStartup) {
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

        companies.add(createCompany(1000L, "Globex Inc", "1 Main St, Metropolis", "Jane Doe", "555-111-2222"));
        companies.add(createCompany(1500L, "Fustion Technologies", "88 Innovation Park, Dublin", "Alex Murphy", "555-444-1212"));
        companies.add(createCompany(2000L, "Initech", "42 Silicon Ave, Tech City", "John Roe", "555-333-4444"));
        companies.add(createCompany(3000L, "Umbrella Financial", "17 Queen Rd, Dublin", "Martha Lane", "555-222-8888"));

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
