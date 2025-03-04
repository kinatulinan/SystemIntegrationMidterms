package com.moreno.oauth2login.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity //Always execute this class file
public class SecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http

                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated()) //Springboot security applies security to all endpoints; mo redirect sa login
                        .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("http://localhost:8080/user-info", true))
                        .logout(logout -> logout.logoutSuccessUrl("/"))
                        .formLogin(form -> form.defaultSuccessUrl("/secured", true))
                        .csrf(AbstractHttpConfigurer::disable)
                .build();


    }
}
