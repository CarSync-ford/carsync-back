package br.com.sprint1.challenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "data-retention")
public class DataRetentionProperties {

    private int hardDeleteRetentionDays = 30;
    private int inactiveUserRetentionYears = 5;
    private String hardDeleteCron = "0 0 2 * * *";
    private String anonymizationCron = "0 0 3 * * 0";
    private boolean enabled = true;

    public int getHardDeleteRetentionDays() {
        return hardDeleteRetentionDays;
    }

    public void setHardDeleteRetentionDays(int hardDeleteRetentionDays) {
        this.hardDeleteRetentionDays = hardDeleteRetentionDays;
    }

    public int getInactiveUserRetentionYears() {
        return inactiveUserRetentionYears;
    }

    public void setInactiveUserRetentionYears(int inactiveUserRetentionYears) {
        this.inactiveUserRetentionYears = inactiveUserRetentionYears;
    }

    public String getHardDeleteCron() {
        return hardDeleteCron;
    }

    public void setHardDeleteCron(String hardDeleteCron) {
        this.hardDeleteCron = hardDeleteCron;
    }

    public String getAnonymizationCron() {
        return anonymizationCron;
    }

    public void setAnonymizationCron(String anonymizationCron) {
        this.anonymizationCron = anonymizationCron;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
