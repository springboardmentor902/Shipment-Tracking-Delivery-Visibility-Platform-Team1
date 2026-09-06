package com.shiptrack.shiptrack_pro.config;
 
import com.shiptrack.shiptrack_pro.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // activates @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {
 
    private final JwtAuthFilter jwtAuthFilter;
 
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/error", "/api/auth/**").permitAll()

                    // The HTTP handshake is public; the STOMP CONNECT frame must carry a valid JWT.
                    .requestMatchers("/api/ws/tracking", "/api/ws/tracking/**").permitAll()

                    .requestMatchers("/api/shipments/**", "/api/routes/**", "/api/route/**", "/api/eta/**")
                            .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR")

                    .requestMatchers(HttpMethod.POST, "/api/notification").hasRole("ADMINISTRATOR")
                    .requestMatchers("/api/notifications/**").authenticated()

                    .requestMatchers(HttpMethod.POST, "/api/pod/**")
                            .hasRole("LOGISTICS_OPERATOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/pod/*/verify")
                            .hasAnyRole("SUPPORT_AGENT", "ADMINISTRATOR")
                    .requestMatchers(HttpMethod.GET, "/api/pod/**", "/api/files/**")
                            .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "SUPPORT_AGENT", "ADMINISTRATOR")
 
                    .requestMatchers("/api/tracking/**")
                            .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
 
                    .requestMatchers("/api/analytics/**", "/api/reports/**")
                            .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")
 
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
 
                    .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
 
        return http.build();
    }
}
