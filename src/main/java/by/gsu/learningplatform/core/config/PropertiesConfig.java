package by.gsu.learningplatform.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LearningPlatformProperties.class)
public class PropertiesConfig {
}
