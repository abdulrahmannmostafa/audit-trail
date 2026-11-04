package com.qeema.util.security.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.qeema.util.security.constants.SecurityConstants;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakRoleConverter keycloakRoleConverter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        final String SWAGGER_UI_PATH = "/swagger-ui.html";
        final String SWAGGER_UI_PATH_WILDCARD = "/swagger-ui/**";
        final String SWAGGER_API_DOCS_PATH_WILDCARD = "/v3/api-docs/**";

        final String PUBLIC_PATH = "/api/public";
        final String ADMIN_PATH = "/api/admin/**";
        final String USER_PATH = "/api/user/**";

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATH).permitAll()
                        .requestMatchers(SWAGGER_UI_PATH, SWAGGER_UI_PATH_WILDCARD, SWAGGER_API_DOCS_PATH_WILDCARD)
                        .permitAll().requestMatchers("/api/admin/audit-logs/**")
                        .hasAnyRole(SecurityConstants.USER_ROLE, SecurityConstants.ADMIN_ROLE)
                        .requestMatchers(ADMIN_PATH).hasRole(SecurityConstants.ADMIN_ROLE)
                        .requestMatchers(USER_PATH)
                        .hasAnyRole(SecurityConstants.USER_ROLE, SecurityConstants.ADMIN_ROLE)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin) {

        final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");
        final List<String> EXPOSED_HEADERS = List.of("Content-Disposition"); // for file downloads

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}