package by.gsu.learningplatform.core.config;

import lombok.val;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Bean("liquibase")
    public SpringLiquibase liquibase(final DataSource dataSource, final LearningPlatformProperties properties) {
        val liquibaseProperties = properties.liquibase();
        val liquibase = new SpringLiquibase();
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
