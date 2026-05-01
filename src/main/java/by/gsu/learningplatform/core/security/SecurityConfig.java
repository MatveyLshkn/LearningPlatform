package by.gsu.learningplatform.core.security;

import lombok.val;
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
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String SUBJECT_CLAIM = "sub";
    private static final String HEALTH_PATH = "/actuator/health";
    private static final String INFO_PATH = "/actuator/info";
    private static final String PROMETHEUS_PATH = "/actuator/prometheus";
    private static final String SWAGGER_PATH = "/swagger-ui.html";
    private static final String SWAGGER_WILDCARD_PATH = "/swagger-ui/**";
    private static final String OPENAPI_PATH = "/openapi/**";
    private static final String REGISTRATIONS_PATH = "/user-registrations";
    private static final String TOKEN_PATH = "/token";
    private static final String USERS_PATH = "/users";
    private static final String PLATFORM_STATISTICS_PATH = "/platform-statistics/**";
    private static final String COURSES_ROOT_PATH = "/courses";
    private static final String COURSE_ID_REGEX = "^/courses/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final String COURSES_PATH = "/courses/**";
    private static final String COURSE_AI_PATH = "/courses/*/ai/**";
    private static final String STUDY_PLAN_PATH = "/courses/*/students/*/ai-study-plan";
    private static final String LESSONS_PATH = "/lessons/**";
    private static final String LECTURES_PATH = "/lectures/**";
    private static final String ASSESSMENTS_PATH = "/assessments/**";
    private static final String SUBMISSIONS_PATH = "/submissions/**";
    private static final String ENROLLMENTS_PATH = "/enrollments/**";
    private static final String ALL_PATHS = "/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorrelationIdFilter correlationIdFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HEALTH_PATH, INFO_PATH, PROMETHEUS_PATH).permitAll()
                        .requestMatchers(SWAGGER_PATH, SWAGGER_WILDCARD_PATH, OPENAPI_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, REGISTRATIONS_PATH, TOKEN_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, COURSES_ROOT_PATH).permitAll()
                        .requestMatchers(new RegexRequestMatcher(COURSE_ID_REGEX, HttpMethod.GET.name())).permitAll()
                        .requestMatchers(HttpMethod.GET, USERS_PATH).hasRole(ADMIN_ROLE)
                        .requestMatchers(PLATFORM_STATISTICS_PATH).hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.POST, COURSES_PATH, LESSONS_PATH, LECTURES_PATH, ASSESSMENTS_PATH).hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.PATCH, COURSES_PATH, LESSONS_PATH, LECTURES_PATH, ASSESSMENTS_PATH, SUBMISSIONS_PATH).hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE, COURSES_PATH, LESSONS_PATH, LECTURES_PATH, ASSESSMENTS_PATH).hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.POST, ENROLLMENTS_PATH, SUBMISSIONS_PATH).hasAnyRole(STUDENT_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE, ENROLLMENTS_PATH).hasAnyRole(STUDENT_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.GET, COURSE_AI_PATH, STUDY_PLAN_PATH).hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.GET, ALL_PATHS).authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        val converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        converter.setPrincipalClaimName(SUBJECT_CLAIM);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(final Jwt jwt) {
        return new RoleClaimConverter().convert(jwt.getClaims());
    }
}
