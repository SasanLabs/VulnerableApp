package org.sasanlabs.configuration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Runs all {@link ModuleSeeder} implementations once the application is ready, ensuring the
 * database is populated before the app accepts requests.
 */
@Component
public class DatabaseSeeder {

    private static final transient Logger LOGGER =
            LogManager.getLogger(DatabaseSeeder.class);

    //  Finds every @Component that implements ModuleSeeder
    private final List<ModuleSeeder> seeders;

    public DatabaseSeeder(List<ModuleSeeder> seeders) {
        this.seeders = seeders;
    }

    @EventListener(ApplicationReadyEvent.class) // Runs when  application is ready
    public void seedAllModules() {
        LOGGER.info("Starting Global Database Seeding");

        for (ModuleSeeder seeder : seeders) {
            if (!seeder.isSeeded()) {
                seeder.seed();
                LOGGER.info("{} has seeded table: {} for module: {}.", seeder.toString(), seeder.getModuleTable(), seeder.getModuleName());
            }
        }

        LOGGER.info("Seeding complete. Processed {} modules", seeders.size());
    }
}
