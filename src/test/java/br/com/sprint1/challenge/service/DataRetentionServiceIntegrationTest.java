package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.config.DataRetentionProperties;
import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Dealership;
import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.repository.AssistantInteractionRepository;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.DealershipRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.ServiceRecordRepository;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DataRetentionServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private AssistantInteractionRepository assistantInteractionRepository;

    @Autowired
    private DealershipRepository dealershipRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @Autowired
    private DataRetentionProperties properties;

    @Autowired
    private MeterRegistry meterRegistry;

    private Clock fixedClock;
    private Instant nowInstant;
    private DataRetentionService retentionService;
    private UserType userRole;

    @BeforeEach
    void setUp() {
        nowInstant = Instant.parse("2026-09-14T10:00:00Z");
        fixedClock = Clock.fixed(nowInstant, ZoneOffset.UTC);

        retentionService = new DataRetentionService(
                properties,
                userRepository,
                customerRepository,
                leadRepository,
                vehicleRepository,
                serviceRecordRepository,
                assistantInteractionRepository,
                fixedClock,
                meterRegistry
        );

        userRole = userTypeRepository.findAll().stream()
                .filter(ut -> "USER".equals(ut.getType()))
                .findFirst()
                .orElseGet(() -> userTypeRepository.save(new UserType("a1b2c3d4-e5f6-7890-abcd-ef1234567890", "USER")));
    }

    @Test
    @DisplayName("purgeHardDeletedRecords should purge records with deleted_at > 30 days and increment counter")
    void purgeHardDeletedRecords_purgesExpiredAndUpdatesMetric() {
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC);
        double initialHardDeleteCount = getCounterCount("hard_delete");

        // 1. Expired soft-deleted user (deleted 35 days ago)
        User expiredUser = new User();
        expiredUser.setUsername("expiredUser");
        expiredUser.setEmail("expired@test.com");
        expiredUser.setCpf("11111111111");
        expiredUser.setHashedPassword("hash");
        expiredUser.setUserType(userRole);
        expiredUser.setDeletedAt(now.minusDays(35));
        expiredUser = userRepository.save(expiredUser);

        // 2. Recent soft-deleted user (deleted 10 days ago - should be kept)
        User recentDeletedUser = new User();
        recentDeletedUser.setUsername("recentDeletedUser");
        recentDeletedUser.setEmail("recent@test.com");
        recentDeletedUser.setCpf("22222222222");
        recentDeletedUser.setHashedPassword("hash");
        recentDeletedUser.setUserType(userRole);
        recentDeletedUser.setDeletedAt(now.minusDays(10));
        recentDeletedUser = userRepository.save(recentDeletedUser);

        // 3. Active user (deletedAt == null - should be kept)
        User activeUser = new User();
        activeUser.setUsername("activeUser");
        activeUser.setEmail("active@test.com");
        activeUser.setCpf("33333333333");
        activeUser.setHashedPassword("hash");
        activeUser.setUserType(userRole);
        activeUser = userRepository.save(activeUser);

        // 4. Expired soft-deleted Customer (deleted 31 days ago) with a Vehicle and Lead
        Dealership dealership = dealershipRepository.save(new Dealership(null, "Test Dealership", "SP", "SP"));
        Customer expiredCustomer = customerRepository.save(new Customer(
                null, "Expired Customer", "expired.customer@test.com", "11999999999", "SP", "SP", dealership.getId()
        ));
        expiredCustomer.setDeletedAt(now.minusDays(31));
        expiredCustomer = customerRepository.save(expiredCustomer);

        Vehicle vehicle = vehicleRepository.save(new Vehicle(
                null, "VIN12345678901234", "Polo", "Hatch", 2022, 10000,
                expiredCustomer.getId(), dealership.getId(), LocalDate.now().plusYears(1), "GOOD"
        ));

        leadRepository.save(new Lead(
                null, expiredCustomer.getId(), vehicle.getId(), dealership.getId(),
                "Customer Lead", "Test", "LOW", "OPEN", "WEB", now.minusDays(40), null
        ));

        // 5. Expired soft-deleted Lead (deleted 32 days ago) on active customer
        Customer activeCustomer = customerRepository.save(new Customer(
                null, "Active Customer", "active.customer@test.com", "11888888888", "SP", "SP", dealership.getId()
        ));
        Lead expiredLead = leadRepository.save(new Lead(
                null, activeCustomer.getId(), null, dealership.getId(),
                "Expired Lead", "Test", "LOW", "OPEN", "WEB", now.minusDays(50), null
        ));
        expiredLead.setDeletedAt(now.minusDays(32));
        expiredLead = leadRepository.save(expiredLead);

        // Execute purge
        long purgedCount = retentionService.purgeHardDeletedRecords();

        // Expired user (1) + Expired customer (1) + Expired lead (1) = 3
        assertEquals(3, purgedCount);

        // Verify database state
        assertFalse(userRepository.findById(expiredUser.getId()).isPresent());
        assertTrue(userRepository.findById(recentDeletedUser.getId()).isPresent());
        assertTrue(userRepository.findById(activeUser.getId()).isPresent());

        assertFalse(customerRepository.findById(expiredCustomer.getId()).isPresent());
        assertFalse(vehicleRepository.findById(vehicle.getId()).isPresent());
        assertFalse(leadRepository.findById(expiredLead.getId()).isPresent());
        assertTrue(customerRepository.findById(activeCustomer.getId()).isPresent());

        // Verify Micrometer counter
        double finalHardDeleteCount = getCounterCount("hard_delete");
        assertEquals(initialHardDeleteCount + 3, finalHardDeleteCount);
    }

    @Test
    @DisplayName("anonymizeInactiveUsers should anonymize users with last_login > 5 years and update metric")
    void anonymizeInactiveUsers_anonymizesOldUsersAndUpdatesMetric() {
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC);
        double initialAnonCount = getCounterCount("anonymization");

        // 1. Inactive user (last login 6 years ago)
        User inactiveUser = new User();
        inactiveUser.setUsername("inactiveOldUser");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setCpf("44444444444");
        inactiveUser.setHashedPassword("secretHashedPassword");
        inactiveUser.setUserType(userRole);
        inactiveUser.setLastLogin(now.minusYears(6));
        inactiveUser.setMfaEnabled(true);
        inactiveUser.setMfaSecret("SECRET123");
        inactiveUser.setRefreshToken("token-abc");
        inactiveUser = userRepository.save(inactiveUser);

        // 2. Active user (last login 1 year ago - should NOT be anonymized)
        User activeRecentUser = new User();
        activeRecentUser.setUsername("activeRecentUser");
        activeRecentUser.setEmail("recent.login@example.com");
        activeRecentUser.setCpf("55555555555");
        activeRecentUser.setHashedPassword("secretHashedPassword");
        activeRecentUser.setUserType(userRole);
        activeRecentUser.setLastLogin(now.minusYears(1));
        activeRecentUser = userRepository.save(activeRecentUser);

        // Execute anonymization
        long anonymizedCount = retentionService.anonymizeInactiveUsers();

        assertEquals(1, anonymizedCount);

        // Verify inactive user was anonymized
        User updatedInactive = userRepository.findById(inactiveUser.getId()).orElseThrow();
        assertTrue(updatedInactive.getUsername().startsWith("anonymized_"));
        assertTrue(updatedInactive.getEmail().endsWith("@anonymized.local"));
        assertTrue(updatedInactive.getCpf().startsWith("ANO"));
        assertEquals(false, updatedInactive.getMfaEnabled());
        assertNull(updatedInactive.getMfaSecret());
        assertNull(updatedInactive.getRefreshToken());

        // Verify active user was NOT changed
        User updatedActive = userRepository.findById(activeRecentUser.getId()).orElseThrow();
        assertEquals("activeRecentUser", updatedActive.getUsername());
        assertEquals("recent.login@example.com", updatedActive.getEmail());
        assertEquals("55555555555", updatedActive.getCpf());

        // Verify Micrometer counter
        double finalAnonCount = getCounterCount("anonymization");
        assertEquals(initialAnonCount + 1, finalAnonCount);

        // Test Idempotence: running again should not re-anonymize
        long secondRunCount = retentionService.anonymizeInactiveUsers();
        assertEquals(0, secondRunCount);
        assertEquals(finalAnonCount, getCounterCount("anonymization"));
    }

    @Test
    @DisplayName("data retention should do nothing when enabled is false")
    void retentionJobs_doNothingWhenDisabled() {
        properties.setEnabled(false);
        try {
            assertEquals(0, retentionService.purgeHardDeletedRecords());
            assertEquals(0, retentionService.anonymizeInactiveUsers());
        } finally {
            properties.setEnabled(true);
        }
    }

    private double getCounterCount(String type) {
        var counter = meterRegistry.find("data_retention.removed")
                .tag("type", type)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }
}
