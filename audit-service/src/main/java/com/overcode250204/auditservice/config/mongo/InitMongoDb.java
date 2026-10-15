package com.overcode250204.auditservice.config.mongo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class InitMongoDb {
    @Bean
    CommandLineRunner initDatabase(MongoTemplate mongoTemplate) {
        return args -> {
            if (!mongoTemplate.collectionExists("audit_log")) {
                mongoTemplate.createCollection("audit_log");
            }
        };
    }
}
