package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.config.DataRetentionProperties;
import br.com.sprint1.challenge.entity.Customer;
import br.com.sprint1.challenge.entity.Lead;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.Vehicle;
import br.com.sprint1.challenge.repository.AssistantInteractionRepository;
import br.com.sprint1.challenge.repository.CustomerRepository;
import br.com.sprint1.challenge.repository.LeadRepository;
import br.com.sprint1.challenge.repository.ServiceRecordRepository;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.VehicleRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final DataRetentionProperties properties;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final AssistantInteractionRepository assistantInteractionRepository;
    private final Clock clock;
    private final Counter hardDeleteCounter;
    private final Counter anonymizationCounter;

    public DataRetentionService(DataRetentionProperties properties,
                                UserRepository userRepository,
                                CustomerRepository customerRepository,
                                LeadRepository leadRepository,
                                VehicleRepository vehicleRepository,
                                ServiceRecordRepository serviceRecordRepository,
                                AssistantInteractionRepository assistantInteractionRepository,
                                Clock clock,
                                MeterRegistry meterRegistry) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.assistantInteractionRepository = assistantInteractionRepository;
        this.clock = clock;

        this.hardDeleteCounter = Counter.builder("data_retention.removed")
                .tag("type", "hard_delete")
                .register(meterRegistry);

        this.anonymizationCounter = Counter.builder("data_retention.removed")
                .tag("type", "anonymization")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${data-retention.hard-delete-cron:0 0 2 * * *}")
    @Transactional
    public long purgeHardDeletedRecords() {
        if (!properties.isEnabled()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(properties.getHardDeleteRetentionDays());
        long removedCount = 0;

        // 1. Purge soft-deleted leads
        List<Lead> expiredLeads = leadRepository.findAllByDeletedAtBefore(cutoff);
        if (!expiredLeads.isEmpty()) {
            leadRepository.deleteAll(expiredLeads);
            removedCount += expiredLeads.size();
        }

        // 2. Purge soft-deleted customers and cascading dependencies
        List<Customer> expiredCustomers = customerRepository.findAllByDeletedAtBefore(cutoff);
        for (Customer customer : expiredCustomers) {
            List<Vehicle> customerVehicles = vehicleRepository.findByCustomerId(customer.getId());
            for (Vehicle vehicle : customerVehicles) {
                assistantInteractionRepository.deleteAll(
                        assistantInteractionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicle.getId())
                );
                serviceRecordRepository.deleteAll(
                        serviceRecordRepository.findByVehicleId(vehicle.getId())
                );
                List<Lead> vehicleLeads = leadRepository.findAll().stream()
                        .filter(l -> Objects.equals(l.getVehicleId(), vehicle.getId()))
                        .toList();
                if (!vehicleLeads.isEmpty()) {
                    leadRepository.deleteAll(vehicleLeads);
                }
                vehicleRepository.delete(vehicle);
            }

            List<Lead> remainingCustomerLeads = leadRepository.findByCustomerId(customer.getId());
            if (!remainingCustomerLeads.isEmpty()) {
                leadRepository.deleteAll(remainingCustomerLeads);
            }

            customerRepository.delete(customer);
            removedCount++;
        }

        // 3. Purge soft-deleted users
        List<User> expiredUsers = userRepository.findAllByDeletedAtBefore(cutoff);
        if (!expiredUsers.isEmpty()) {
            userRepository.deleteAll(expiredUsers);
            removedCount += expiredUsers.size();
        }

        if (removedCount > 0) {
            hardDeleteCounter.increment(removedCount);
            log.info("DATA_RETENTION purge completed. Total records removed: {}", removedCount);
        }

        return removedCount;
    }

    @Scheduled(cron = "${data-retention.anonymization-cron:0 0 3 * * 0}")
    @Transactional
    public long anonymizeInactiveUsers() {
        if (!properties.isEnabled()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusYears(properties.getInactiveUserRetentionYears());
        List<User> inactiveUsers = userRepository.findAllByLastLoginBefore(cutoff);
        long anonymizedCount = 0;

        for (User user : inactiveUsers) {
            if (user.getUsername() != null && user.getUsername().startsWith("anonymized_")) {
                continue; // already anonymized
            }

            String id = user.getId();
            String idClean = id != null ? id.replace("-", "") : "0000000";
            String suffix = idClean.substring(0, Math.min(8, idClean.length()));

            user.setUsername("anonymized_" + suffix);
            user.setEmail("anonymized_" + id + "@anonymized.local");
            user.setCpf("ANO" + suffix);
            user.setHashedPassword("$2a$10$ANONYMIZED.DATA.RETENTION.HASH............");
            user.setRefreshToken(null);
            user.setMfaSecret(null);
            user.setMfaEnabled(false);

            userRepository.save(user);
            anonymizedCount++;
        }

        if (anonymizedCount > 0) {
            anonymizationCounter.increment(anonymizedCount);
            log.info("DATA_RETENTION anonymization completed. Total users anonymized: {}", anonymizedCount);
        }

        return anonymizedCount;
    }
}
