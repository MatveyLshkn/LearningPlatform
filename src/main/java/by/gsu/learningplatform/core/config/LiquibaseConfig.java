package by.gsu.learningplatform.core.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Bean("liquibase")
    public SpringLiquibase liquibase(DataSource dataSource, LearningPlatformProperties properties) {
        final var liquibaseProperties = properties.liquibase();
        final var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(liquibaseProperties.changeLog());
        liquibase.setShouldRun(liquibaseProperties.enabled());
        return liquibase;
    }

    @Configuration(proxyBeanMethods = false)
    static class LiquibaseJpaDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {
        LiquibaseJpaDependencyConfig() {
            super("liquibase");
        }
    }
}
