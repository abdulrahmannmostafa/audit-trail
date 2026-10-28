package com.qeema.util.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("repair")
public class FlywayRepairRunner implements CommandLineRunner {

    private final Flyway flyway;

    public FlywayRepairRunner(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(String... args) {
        flyway.repair();
        System.out.println("Flyway repair completed successfully");
    }
}