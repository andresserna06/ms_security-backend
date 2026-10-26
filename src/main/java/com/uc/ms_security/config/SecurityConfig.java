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

                .authorizeHttpRequests(auth -> auth // Define quién puede entrar a qué lugares:
                        .requestMatchers(
                                "/api/auth/login" // Deja la puerta del login abierta al público. Cualquiera puede intentar entrar sin tener un token previo (de lo contrario, ¡nadie podría iniciar sesión!)
                        ).permitAll()

                        .anyRequest().authenticated() // Cierra con candado absoluto todas las demás rutas de tu API. Nadie pasa sin estar autenticado
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(
                                Customizer.withDefaults()
                        )
                );

        return http.build();
    }
}
