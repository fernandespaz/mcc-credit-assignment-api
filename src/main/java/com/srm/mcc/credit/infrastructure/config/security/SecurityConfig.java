package com.srm.mcc.credit.infrastructure.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Stateless JWT-based security for the whole API.
 * <p>
 * Authorization matrix:
 * <ul>
 *   <li>{@code /api/v1/auth/**}, health check and API docs — public.</li>
 *   <li>{@code ADMIN} — full access, including assignor and exchange-rate management (PII / rate
 *       manipulation are the most sensitive write operations).</li>
 *   <li>{@code ADMIN}/{@code OPERATOR} — register receivables and execute settlements (money
 *       movement).</li>
 *   <li>Any authenticated role ({@code ADMIN}/{@code OPERATOR}/{@code VIEWER}) — read-only
 *       (GET) endpoints and reports.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Assignors (PII) and exchange rates: ADMIN-only writes
                        .requestMatchers(HttpMethod.POST, "/api/v1/assignors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/assignors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/assignors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/exchange-rates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/exchange-rates/**").hasRole("ADMIN")

                        // Receivables and settlements: ADMIN or OPERATOR can write (money movement)
                        .requestMatchers(HttpMethod.POST, "/api/v1/receivables/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/settlements/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Remaining actuator endpoints: ADMIN-only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Everything else under the API requires an authenticated principal
                        // (covers all GET/read endpoints and reports for ADMIN/OPERATOR/VIEWER)
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJsonError(response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJsonError(response, 403, "Access denied")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("status", status, "message", message, "timestamp", LocalDateTime.now().toString())));
    }
}
