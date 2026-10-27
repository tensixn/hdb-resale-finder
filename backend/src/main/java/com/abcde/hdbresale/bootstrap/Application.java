package com.abcde.hdbresale.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Start-up class: boots the Spring context for the whole system. */
@SpringBootApplication(scanBasePackages = "com.abcde.hdbresale")
@EnableJpaRepositories(basePackages = "com.abcde.hdbresale.repository")
@EntityScan(basePackages = "com.abcde.hdbresale.domain")
@ConfigurationPropertiesScan(basePackages = "com.abcde.hdbresale.config")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
