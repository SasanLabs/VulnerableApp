package org.sasanlabs.configuration;

import java.util.Arrays;
import java.util.List;
import org.sasanlabs.service.vulnerability.nosqlInjection.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Seeds the embedded MongoDB instance with users used by the NoSQL injection module. */
@Configuration
public class MongoConfiguration {

    @Bean
    public CommandLineRunner seedUsers(MongoTemplate mongoTemplate) {
        return args -> {
            mongoTemplate.dropCollection(User.class);
            List<User> users =
                    Arrays.asList(new User("admin", "s3cret"), new User("bob", "hunter2"));
            mongoTemplate.insertAll(users);
        };
    }
}
