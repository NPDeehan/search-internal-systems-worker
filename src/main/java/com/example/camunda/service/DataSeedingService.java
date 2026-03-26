package com.example.camunda.service;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.model.TrustLevel;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.repository.ExternalCompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class DataSeedingService implements CommandLineRunner {

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

    private static final Logger log = LoggerFactory.getLogger(DataSeedingService.class);

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ExternalCompanyRepository externalCompanyRepository;

    private final Random random = new Random();

    public DataSeedingService(EmployeeRepository employeeRepository,
                             CustomerRepository customerRepository,
                             ExternalCompanyRepository externalCompanyRepository) {
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.externalCompanyRepository = externalCompanyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting data seeding...");
        
        if (employeeRepository.count() < 50) {
            seedEmployees();
        }
        
        if (customerRepository.count() < 100) {
            seedCustomers();
        }
        
        // Always ensure Johnathan Doe exists
        ensureJohnathanDoeExists();
        ensureNiallDeehanExists();
        
        if (externalCompanyRepository.count() < 100) {
            seedExternalCompanies();
        }
        
        log.info("Data seeding completed. Employees: {}, Customers: {}, Companies: {}", 
                employeeRepository.count(), customerRepository.count(), externalCompanyRepository.count());
    }

    private void seedEmployees() {
        log.info("Seeding employees...");
        
        String[] departments = {
            "Engineering", "Sales", "Marketing", "Human Resources", "Finance", "Operations",
            "Customer Service", "Product Management", "Quality Assurance", "IT Support",
            "Business Development", "Legal", "Research & Development", "Supply Chain"
        };
        
        String[] jobTitles = {
            "Software Engineer", "Senior Developer", "Product Manager", "Sales Representative",
            "Marketing Specialist", "HR Manager", "Financial Analyst", "Operations Manager",
            "Customer Success Manager", "QA Engineer", "Business Analyst", "Technical Lead",
            "Account Manager", "Marketing Director", "HR Business Partner", "Data Scientist",
            "DevOps Engineer", "Sales Manager", "Content Marketing Manager", "Legal Counsel"
        };

        for (long i = 1; i <= 50; i++) {
            Employee employee = new Employee();
            employee.setEmployeeId(i);
            employee.setFullName(getRandomElement(GLOBAL_FIRST_NAMES) + " " + getRandomElement(GLOBAL_LAST_NAMES));
            employee.setJobTitle(getRandomElement(jobTitles));
            employee.setDepartment(getRandomElement(departments));
            employee.setPhoneNumber(generatePhoneNumber());
            
            employeeRepository.save(employee);
        }
        
        log.info("Seeded 50 employees");
    }

    private void seedCustomers() {
        log.info("Seeding customers...");
        
        // Get all employee IDs to assign customers to them
        List<Long> employeeIds = employeeRepository.findAll().stream()
                .map(Employee::getEmployeeId)
                .toList();
        
        // Always seed "Johnathan Doe" as the first customer
        Customer johnathanDoe = new Customer();
        johnathanDoe.setCustomerId(1L);
        johnathanDoe.setCustomerName("Johnathan Doe");
        johnathanDoe.setAddress("1 Example Lane, Dublin, Ireland");
        johnathanDoe.setEmployeeId(getRandomElement(employeeIds));
        johnathanDoe.setTrustLevel(TrustLevel.L1);
        johnathanDoe.setEmail(buildFakeCustomerEmail(johnathanDoe.getCustomerName(), johnathanDoe.getCustomerId()));
        customerRepository.save(johnathanDoe);
        log.info("Seeded special customer: Johnathan Doe (ID: 1)");
        
        for (long i = 2; i <= 100; i++) {
            Customer customer = new Customer();
            customer.setCustomerId(i);
            customer.setCustomerName(getRandomElement(GLOBAL_FIRST_NAMES) + " " + getRandomElement(GLOBAL_LAST_NAMES));
            CityCountry location = getRandomElement(GLOBAL_LOCATIONS);
            customer.setAddress(buildDiverseAddress((int) (100 + i), (int) i, location));
            // Randomly assign to an employee
            customer.setEmployeeId(getRandomElement(employeeIds));
            customer.setTrustLevel(getRandomTrustLevel());
            customer.setEmail(buildFakeCustomerEmail(customer.getCustomerName(), customer.getCustomerId()));
            
            customerRepository.save(customer);
        }
        
        log.info("Seeded 100 customers");
    }
    
    private void ensureJohnathanDoeExists() {
        log.info("Ensuring Johnathan Doe exists in customer database...");
        
        // Check if Johnathan Doe already exists
        List<Customer> existingCustomers = customerRepository.findAll();
        boolean johnathanExists = existingCustomers.stream()
                .anyMatch(customer -> "Johnathan Doe".equalsIgnoreCase(customer.getCustomerName()));
        
        if (!johnathanExists) {
            // Find the next available customer ID
            Long maxId = existingCustomers.stream()
                    .mapToLong(Customer::getCustomerId)
                    .max()
                    .orElse(0L);
            
            // Get a random employee ID to assign
            List<Long> availableEmployeeIds = employeeRepository.findAll().stream()
                    .map(Employee::getEmployeeId)
                    .toList();
            
            Customer johnathanDoe = new Customer();
            johnathanDoe.setCustomerId(maxId + 1);
            johnathanDoe.setCustomerName("Johnathan Doe");
            johnathanDoe.setAddress("1 Example Lane, Dublin, Ireland");
            johnathanDoe.setEmployeeId(getRandomElement(availableEmployeeIds));
            johnathanDoe.setTrustLevel(TrustLevel.L1);
            johnathanDoe.setEmail(buildFakeCustomerEmail(johnathanDoe.getCustomerName(), johnathanDoe.getCustomerId()));
            
            customerRepository.save(johnathanDoe);
            log.info("Created Johnathan Doe customer with ID: {}", johnathanDoe.getCustomerId());
        } else {
            log.info("Johnathan Doe already exists in customer database");
        }
    }

    private void ensureNiallDeehanExists() {
        log.info("Ensuring {} exists in customer database...", NIAL_DEEHAN_NAME);

        List<Customer> existingCustomers = customerRepository.findAll();
        Customer niall = existingCustomers.stream()
                .filter(customer -> customer.getCustomerName() != null && NIAL_DEEHAN_NAME.equalsIgnoreCase(customer.getCustomerName().trim()))
                .findFirst()
                .orElse(null);

        if (niall == null) {
            Long maxId = existingCustomers.stream()
                    .mapToLong(Customer::getCustomerId)
                    .max()
                    .orElse(0L);

            List<Long> availableEmployeeIds = employeeRepository.findAll().stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            Customer niallCustomer = new Customer();
            niallCustomer.setCustomerId(maxId + 1);
            niallCustomer.setCustomerName(NIAL_DEEHAN_NAME);
            niallCustomer.setAddress("14 Fitzwilliam Square, Dublin, Ireland");
            niallCustomer.setEmployeeId(getRandomElement(availableEmployeeIds));
            niallCustomer.setTrustLevel(TrustLevel.L1);
            niallCustomer.setEmail(NIAL_DEEHAN_EMAIL);

            customerRepository.save(niallCustomer);
            log.info("Created {} customer with ID: {}", NIAL_DEEHAN_NAME, niallCustomer.getCustomerId());
            return;
        }

        if (!NIAL_DEEHAN_EMAIL.equalsIgnoreCase(niall.getEmail())) {
            niall.setEmail(NIAL_DEEHAN_EMAIL);
            customerRepository.save(niall);
            log.info("Updated {} email to {}", NIAL_DEEHAN_NAME, NIAL_DEEHAN_EMAIL);
        } else {
            log.info("{} already exists with required email", NIAL_DEEHAN_NAME);
        }
    }

    private void seedExternalCompanies() {
        log.info("Seeding external companies...");

        for (long i = 1; i <= 100; i++) {
            ExternalCompany company = new ExternalCompany();
            company.setCompanyId(i);
            String name = getRandomElement(COMPANY_NAME_PREFIXES) + " "
                    + getRandomElement(COMPANY_NAME_MIDDLES) + " "
                    + getRandomElement(COMPANY_NAME_SUFFIXES);
            company.setCompanyName(name + (i > 96 ? " " + i : ""));

            CityCountry location = getRandomElement(GLOBAL_LOCATIONS);
            company.setAddress(buildDiverseAddress(random.nextInt(9999) + 1, (int) i * 3, location));
            
            company.setContactPerson(generateContactPerson());
            company.setPhoneNumber(generatePhoneNumber(location.country()));
            
            externalCompanyRepository.save(company);
        }
        
        log.info("Seeded 100 external companies");
    }

    private String getRandomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }

    private <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
    
    private <T> T getRandomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private TrustLevel getRandomTrustLevel() {
        return switch (random.nextInt(3)) {
            case 0 -> TrustLevel.L1;
            case 1 -> TrustLevel.L2;
            default -> TrustLevel.L3;
        };
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

    private String generatePhoneNumber() {
        String[] dialCodes = {"353", "234", "91", "55", "81", "82", "52", "971", "254", "49", "45", "64", "90", "27", "56"};
        String dialCode = getRandomElement(dialCodes);
        return "+%s-%03d-%03d-%04d".formatted(
                dialCode,
                200 + random.nextInt(800),
                200 + random.nextInt(800),
                1000 + random.nextInt(9000));
    }

    private String generatePhoneNumber(String country) {
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
        return "+%s-%03d-%03d-%04d".formatted(
            dialCode,
                200 + random.nextInt(800),
                200 + random.nextInt(800),
            1000 + random.nextInt(9000));
    }

    private String generateContactPerson() {
        String[] titles = {"Ms.", "Mr.", "Dr.", "Prof."};
        
        return getRandomElement(titles) + " " + getRandomElement(GLOBAL_FIRST_NAMES) + " " + getRandomElement(GLOBAL_LAST_NAMES);
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

    private record CityCountry(String city, String country) {
    }
}
