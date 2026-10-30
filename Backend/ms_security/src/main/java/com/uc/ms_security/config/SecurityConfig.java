package com.uc.ms_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { 
        return new BCryptPasswordEncoder(12); // Define que el sistema usará el algoritmo BCrypt (con nivel de fuerza 12) para proteger las contraseñas
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS // El servidor no recordará quién inició sesión hace cinco minutos. Por lo tanto, el usuario está obligado a presentar su token JWT en cada una de las peticiones que haga.
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/verify-2fa",
                                "/api/auth/logout"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(
                                Customizer.withDefaults()
                        )
                );

        return http.build();
    }
}
