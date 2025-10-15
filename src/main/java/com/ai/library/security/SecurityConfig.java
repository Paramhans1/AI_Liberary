package com.ai.library.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ai.library.repository.UserRepository;
import org.springframework.core.env.Environment;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final Environment env;

    public SecurityConfig(UserRepository userRepository, Environment env) {
        this.userRepository = userRepository;
        this.env = env;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CustomUserDetailsService customUserDetailsService() {
        return new CustomUserDetailsService(userRepository);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .exceptionHandling().authenticationEntryPoint(new AuthEntryPoint()).and()
            .authorizeHttpRequests((authorize) -> {
                // Use Ant-style matchers to avoid ambiguity when multiple servlets (H2 console) are present
                authorize.requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/auth/**"),
                                         new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/h2-console/**"),
                                         new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/webhooks/**")).permitAll();

                // Permit actuator only during tests (test profile active)
                if (env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("test")) {
                    authorize.requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/actuator/**")).permitAll();
                }

                authorize.anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers().frameOptions().disable(); // for H2 console

        return http.build();
    }
}
