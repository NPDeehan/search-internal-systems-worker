package com.example.camunda.service;

import com.example.camunda.model.Customer;
import com.example.camunda.model.Employee;
import com.example.camunda.model.ExternalCompany;
import com.example.camunda.repository.CustomerRepository;
import com.example.camunda.repository.EmployeeRepository;
import com.example.camunda.repository.ExternalCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeedingService implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ExternalCompanyRepository externalCompanyRepository;

    private final Random random = new Random();

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
        
        if (externalCompanyRepository.count() < 100) {
            seedExternalCompanies();
        }
        
        log.info("Data seeding completed. Employees: {}, Customers: {}, Companies: {}", 
                employeeRepository.count(), customerRepository.count(), externalCompanyRepository.count());
    }

    private void seedEmployees() {
        log.info("Seeding employees...");
        
        String[] firstNames = {
            "James", "Mary", "Robert", "Patricia", "John", "Jennifer", "Michael", "Linda",
            "David", "Elizabeth", "William", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Christopher", "Karen", "Charles", "Lisa", "Daniel", "Nancy",
            "Matthew", "Betty", "Anthony", "Helen", "Mark", "Sandra", "Donald", "Donna",
            "Steven", "Carol", "Paul", "Ruth", "Andrew", "Sharon", "Joshua", "Michelle",
            "Kenneth", "Laura", "Kevin", "Sarah", "Brian", "Kimberly", "George", "Deborah",
            "Timothy", "Dorothy", "Ronald", "Lisa", "Jason", "Nancy", "Edward", "Karen"
        };
        
        String[] lastNames = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
            "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
            "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
            "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
            "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker"
        };
        
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
            employee.setFullName(getRandomElement(firstNames) + " " + getRandomElement(lastNames));
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
        johnathanDoe.setEmployeeId(getRandomElement(employeeIds));
        customerRepository.save(johnathanDoe);
        log.info("Seeded special customer: Johnathan Doe (ID: 1)");
        
        String[] customerFirstNames = {
            "Alice", "Bob", "Charlie", "Diana", "Edward", "Fiona", "George", "Hannah",
            "Ian", "Julia", "Kevin", "Laura", "Mike", "Nina", "Oscar", "Paula",
            "Quincy", "Rachel", "Steve", "Tina", "Ulrich", "Vera", "Walter", "Xara",
            "Yolanda", "Zachary", "Amanda", "Benjamin", "Catherine", "Douglas", "Emily", "Frank",
            "Grace", "Henry", "Irene", "Jack", "Katherine", "Leonard", "Margaret", "Nathan",
            "Olivia", "Peter", "Quinn", "Rebecca", "Samuel", "Teresa", "Ursula", "Victor",
            "Wendy", "Xavier", "Yvonne", "Zoe", "Aaron", "Brenda", "Carlos", "Debbie",
            "Eric", "Felicia", "Gerald", "Holly", "Isaac", "Janet", "Keith", "Louise",
            "Martin", "Nicole", "Owen", "Pamela", "Roger", "Stephanie", "Tony", "Valerie",
            "Wayne", "Ximena", "Yale", "Zara", "Albert", "Beatrice", "Colin", "Denise",
            "Edgar", "Florence", "Gary", "Helen", "Ivan", "Joyce", "Karl", "Linda",
            "Mason", "Norma", "Otto", "Priscilla", "Quentin", "Rosa", "Simon", "Theresa",
            "Ulysses", "Victoria", "Warren", "Xenia", "York", "Zelda"
        };
        
        String[] customerLastNames = {
            "Cooper", "Reed", "Bailey", "Bell", "Murphy", "Rivera", "Cook", "Rogers", "Morgan", "Peterson",
            "Collins", "Edwards", "Stewart", "Flores", "Morris", "Nguyen", "Parker", "Gonzalez", "Alexander", "Ramos",
            "Wallace", "Griffin", "West", "Cole", "Hayes", "Chavez", "Gibson", "Bryant", "Ellis", "Stevens",
            "Murray", "Ford", "Marshall", "Owens", "McDonald", "Harrison", "Ruiz", "Kennedy", "Wells", "Alvarez",
            "Woods", "Mendez", "Castillo", "Olson", "Webb", "Washington", "Tucker", "Freeman", "Burns", "Henry",
            "Vasquez", "Snyder", "Simpson", "Crawford", "Jimenez", "Porter", "Mason", "Shaw", "Gordon", "Wagner",
            "Hunter", "Romero", "Hicks", "Dixon", "Hunt", "Palmer", "Robertson", "Black", "Holmes", "Stone",
            "Meyer", "Boyd", "Mills", "Warren", "Fox", "Rose", "Rice", "Morales", "Schmidt", "Patel",
            "Ferguson", "Nichols", "Herrera", "Medina", "Ryan", "Fernandez", "Weaver", "Daniels", "Stephens", "Gardner",
            "Payne", "Kelley", "Dunn", "Pierce", "Arnold", "Tran", "Spencer", "Peters", "Hawkins", "Grant"
        };

        for (long i = 2; i <= 100; i++) {
            Customer customer = new Customer();
            customer.setCustomerId(i);
            customer.setCustomerName(getRandomElement(customerFirstNames) + " " + getRandomElement(customerLastNames));
            // Randomly assign to an employee
            customer.setEmployeeId(getRandomElement(employeeIds));
            
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
            johnathanDoe.setEmployeeId(getRandomElement(availableEmployeeIds));
            
            customerRepository.save(johnathanDoe);
            log.info("Created Johnathan Doe customer with ID: {}", johnathanDoe.getCustomerId());
        } else {
            log.info("Johnathan Doe already exists in customer database");
        }
    }

    private void seedExternalCompanies() {
        log.info("Seeding external companies...");
        
        String[] companyNames = {
            "TechCorp Solutions", "Global Dynamics Inc", "Innovation Labs LLC", "Digital Frontiers Corp",
            "NextGen Technologies", "Alpha Systems", "Beta Innovations", "Gamma Solutions",
            "Delta Enterprises", "Epsilon Corp", "Zeta Technologies", "Eta Innovations",
            "Theta Systems", "Iota Solutions", "Kappa Enterprises", "Lambda Corp",
            "Mu Technologies", "Nu Innovations", "Xi Systems", "Omicron Solutions",
            "Pi Enterprises", "Rho Corp", "Sigma Technologies", "Tau Innovations",
            "Upsilon Systems", "Phi Solutions", "Chi Enterprises", "Psi Corp",
            "Omega Technologies", "Acme Industries", "Zenith Corp", "Pinnacle Solutions",
            "Summit Technologies", "Apex Innovations", "Vertex Systems", "Prime Solutions",
            "Elite Enterprises", "Superior Corp", "Premium Technologies", "Excellence Inc",
            "Quantum Dynamics", "Fusion Technologies", "Synergy Solutions", "Catalyst Corp",
            "Momentum Inc", "Velocity Systems", "Accelerate Solutions", "Breakthrough Technologies",
            "Pioneer Corp", "Frontier Solutions", "Horizon Technologies", "Vista Innovations",
            "Spectrum Systems", "Prism Solutions", "Crystal Technologies", "Diamond Corp",
            "Platinum Inc", "Gold Standard Solutions", "Silver Technologies", "Bronze Systems",
            "Iron Corp", "Steel Solutions", "Titanium Technologies", "Carbon Innovations",
            "Silicon Systems", "Digital Solutions", "Cyber Technologies", "Data Corp",
            "Cloud Innovations", "Web Systems", "Mobile Solutions", "Software Technologies",
            "Hardware Corp", "Network Solutions", "Security Technologies", "Analytics Inc",
            "Intelligence Systems", "Smart Solutions", "Connected Technologies", "IoT Corp",
            "AI Innovations", "Machine Learning Systems", "Robotics Solutions", "Automation Technologies",
            "Blockchain Corp", "Crypto Solutions", "FinTech Technologies", "EdTech Innovations",
            "HealthTech Systems", "BioTech Solutions", "GreenTech Technologies", "CleanTech Corp",
            "RenewTech Inc", "EcoTech Solutions", "SustainTech Technologies", "FutureTech Innovations",
            "ModernTech Systems", "AdvancedTech Solutions", "CuttingEdge Technologies", "StateOfArt Corp",
            "Revolutionary Inc", "Disruptive Solutions", "Transformative Technologies", "Paradigm Innovations"
        };
        
        String[] cities = {
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
            "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville",
            "Fort Worth", "Columbus", "Charlotte", "San Francisco", "Indianapolis", "Seattle",
            "Denver", "Washington", "Boston", "Nashville", "Baltimore", "Oklahoma City",
            "Louisville", "Portland", "Las Vegas", "Milwaukee", "Albuquerque", "Tucson",
            "Fresno", "Sacramento", "Mesa", "Kansas City", "Atlanta", "Omaha",
            "Colorado Springs", "Raleigh", "Virginia Beach", "Miami", "Tampa", "Minneapolis",
            "Tulsa", "Arlington", "New Orleans", "Wichita", "Cleveland", "Bakersfield"
        };
        


        for (long i = 1; i <= 100; i++) {
            ExternalCompany company = new ExternalCompany();
            company.setCompanyId(i);
            company.setCompanyName(getRandomElement(companyNames) + (i > 96 ? " " + i : ""));
            
            String city = getRandomElement(cities);
            String state = getStateForCity(city);
            company.setAddress(random.nextInt(9999) + 1 + " " + getRandomStreetName() + ", " + city + ", " + state + " " + (10000 + random.nextInt(90000)));
            
            company.setContactPerson(generateContactPerson());
            company.setPhoneNumber(generatePhoneNumber());
            
            externalCompanyRepository.save(company);
        }
        
        log.info("Seeded 100 external companies");
    }

    private String getRandomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }
    
    private <T> T getRandomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private String generatePhoneNumber() {
        return String.format("+1-%03d-%03d-%04d", 
                200 + random.nextInt(800), 
                200 + random.nextInt(800), 
                1000 + random.nextInt(9000));
    }

    private String generateContactPerson() {
        String[] titles = {"Mr.", "Ms.", "Dr.", "Prof."};
        String[] firstNames = {"John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Jennifer"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"};
        
        return getRandomElement(titles) + " " + getRandomElement(firstNames) + " " + getRandomElement(lastNames);
    }

    private String getRandomStreetName() {
        String[] streetNames = {
            "Main St", "Oak Ave", "First St", "Second St", "Park Ave", "Elm St", "Washington St",
            "Maple Ave", "Cedar St", "Pine St", "Lake St", "Hill St", "Church St", "High St",
            "School St", "Center St", "Market St", "Water St", "Union St", "Broadway"
        };
        return getRandomElement(streetNames);
    }

    private String getStateForCity(String city) {
        // Simplified state mapping for major cities
        switch (city) {
            case "New York":
                return "NY";
            case "Los Angeles":
            case "San Diego":
            case "San Jose":
            case "San Francisco":
            case "Fresno":
            case "Sacramento":
            case "Bakersfield":
                return "CA";
            case "Chicago":
                return "IL";
            case "Houston":
            case "San Antonio":
            case "Dallas":
            case "Fort Worth":
            case "Austin":
            case "Arlington":
                return "TX";
            case "Phoenix":
            case "Mesa":
            case "Tucson":
                return "AZ";
            case "Philadelphia":
                return "PA";
            case "Jacksonville":
            case "Miami":
            case "Tampa":
                return "FL";
            case "Columbus":
            case "Cleveland":
                return "OH";
            case "Charlotte":
            case "Raleigh":
                return "NC";
            case "Indianapolis":
                return "IN";
            case "Seattle":
                return "WA";
            case "Denver":
            case "Colorado Springs":
                return "CO";
            case "Washington":
                return "DC";
            case "Boston":
                return "MA";
            case "Nashville":
                return "TN";
            case "Baltimore":
                return "MD";
            case "Oklahoma City":
            case "Tulsa":
                return "OK";
            case "Louisville":
                return "KY";
            case "Portland":
                return "OR";
            case "Las Vegas":
                return "NV";
            case "Milwaukee":
                return "WI";
            case "Albuquerque":
                return "NM";
            case "Kansas City":
                return "MO";
            case "Atlanta":
                return "GA";
            case "Omaha":
                return "NE";
            case "Virginia Beach":
                return "VA";
            case "Minneapolis":
                return "MN";
            case "New Orleans":
                return "LA";
            case "Wichita":
                return "KS";
            default:
                return "CA";
        }
    }
}
