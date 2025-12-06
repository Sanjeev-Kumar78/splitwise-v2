package com.example.splitwise;
import org.springframework.http.HttpMethod;

import com.example.splitwise.service.JwtService;
import com.example.splitwise.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;
    private final String frontendOrigin;

    public SecurityConfig(JwtService jwtService,
                          MyUserDetailsService userDetailsService,
                          @Value("${app.frontend.url:https://spliteaseapp.atul.codes}") String frontendOrigin) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.frontendOrigin = frontendOrigin;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                // CORS must be enabled before CSRF/authorize configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for stateless JWT REST API
                .csrf(csrf -> csrf.disable())
                // Authorize requests using lambda style
                .authorizeHttpRequests(auth -> auth
                        // public endpoints
                        .requestMatchers("/api/auth/**", "/api/users/ping", "/h2-console/**", "/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // everything else requires authentication
                        .anyRequest().authenticated()
                )
                // stateless session (no HTTP session)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // add JWT filter before username/password filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // allow frames (H2 console) in dev
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration to allow frontend to call API and send Authorization header.
     * Adjust allowedOrigins / allowedMethods as needed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration conf = new CorsConfiguration();
        conf.setAllowedOrigins(List.of(frontendOrigin));        // allow your frontend origin
        conf.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        conf.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With"));
        conf.setExposedHeaders(List.of("Authorization","Content-Type")); // headers that frontend can read
        conf.setAllowCredentials(true); // set to true if frontend needs cookies (not used for JWT)
        conf.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", conf);
        return src;
    }
}
