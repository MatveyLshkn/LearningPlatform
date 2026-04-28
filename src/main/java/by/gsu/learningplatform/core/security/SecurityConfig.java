package by.gsu.learningplatform.core.security;

import by.gsu.learningplatform.core.observability.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String adminRole = "ADMIN";
    private static final String teacherRole = "TEACHER";
    private static final String studentRole = "STUDENT";
    private static final String subjectClaim = "sub";
    private static final String healthPath = "/actuator/health";
    private static final String infoPath = "/actuator/info";
    private static final String prometheusPath = "/actuator/prometheus";
    private static final String swaggerPath = "/swagger-ui.html";
    private static final String swaggerWildcardPath = "/swagger-ui/**";
    private static final String openapiPath = "/openapi/**";
    private static final String registrationsPath = "/user-registrations";
    private static final String tokenPath = "/token";
    private static final String usersPath = "/users";
    private static final String platformStatisticsPath = "/platform-statistics/**";
    private static final String coursesPath = "/courses/**";
    private static final String courseAiPath = "/courses/*/ai/**";
    private static final String studyPlanPath = "/courses/*/students/*/ai-study-plan";
    private static final String lessonsPath = "/lessons/**";
    private static final String lecturesPath = "/lectures/**";
    private static final String assessmentsPath = "/assessments/**";
    private static final String submissionsPath = "/submissions/**";
    private static final String enrollmentsPath = "/enrollments/**";
    private static final String allPaths = "/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorrelationIdFilter correlationIdFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(healthPath, infoPath, prometheusPath).permitAll()
                        .requestMatchers(swaggerPath, swaggerWildcardPath, openapiPath).permitAll()
                        .requestMatchers(HttpMethod.POST, registrationsPath, tokenPath).permitAll()
                        .requestMatchers(HttpMethod.GET, usersPath).hasRole(adminRole)
                        .requestMatchers(platformStatisticsPath).hasRole(adminRole)
                        .requestMatchers(HttpMethod.POST, coursesPath, lessonsPath, lecturesPath, assessmentsPath).hasAnyRole(teacherRole, adminRole)
                        .requestMatchers(HttpMethod.PATCH, coursesPath, submissionsPath).hasAnyRole(teacherRole, adminRole)
                        .requestMatchers(HttpMethod.DELETE, coursesPath, lessonsPath, lecturesPath).hasAnyRole(teacherRole, adminRole)
                        .requestMatchers(HttpMethod.POST, enrollmentsPath, submissionsPath).hasAnyRole(studentRole, adminRole)
                        .requestMatchers(HttpMethod.DELETE, enrollmentsPath).hasAnyRole(studentRole, adminRole)
                        .requestMatchers(HttpMethod.GET, courseAiPath, studyPlanPath).hasAnyRole(teacherRole, adminRole)
                        .requestMatchers(HttpMethod.GET, allPaths).authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        converter.setPrincipalClaimName(subjectClaim);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return new RoleClaimConverter().convert(jwt.getClaims());
    }
}
