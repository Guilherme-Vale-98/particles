package com.gui.particles.config;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {
    @Value("${jwt.issuer-uri}")
    private String jwtIssuerUri;

    @Value("${github.clientId}")
    private String gitHubClientId;
    @Value("${github.secret}")
    private String gitHubSecret;

    @Value("${customJwt.jwksUri}")
    private String customJwksUri;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.oauth2ResourceServer(
                oauth2 -> oauth2.authenticationManagerResolver(
                        authenticationManageResolver(googleJwtDecoder(), opaqueTokenIntrospector(), customJwtDecoder())
                )
        );

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/articles", "/api/v1/articles/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/articles").permitAll()
                .requestMatchers("/auth/admin").hasRole("ADMIN")
                .requestMatchers("/auth/me", "/auth/user").authenticated()
                .anyRequest().authenticated()
        );

        return http.build();
    }

    private AuthenticationManagerResolver<HttpServletRequest> authenticationManageResolver(
            JwtDecoder googleJwtDecoder, OpaqueTokenIntrospector opaqueTokenIntrospector, JwtDecoder customJwtDecoder) {

        JwtAuthenticationProvider googleJwtAuth = new JwtAuthenticationProvider(googleJwtDecoder);
        googleJwtAuth.setJwtAuthenticationConverter(googleJwtAuthenticationConverter());
        AuthenticationManager googleAuthManager = new ProviderManager(googleJwtAuth);

        AuthenticationManager opaqueAuth = new ProviderManager(
                new OpaqueTokenAuthenticationProvider(opaqueTokenIntrospector)
        );

        AuthenticationManager customJwtAuth = new ProviderManager(
                customJwtAuthProvider()
        );

        return (request) -> {
            if ("google".equals(request.getHeader("type"))) {
                return googleAuthManager;
            }
            if ("github".equals(request.getHeader("type"))) {
                return opaqueAuth;
            }
            return customJwtAuth;
        };
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return new GitHubOpaqueTokenIntrospector(gitHubClientId, gitHubSecret);
    }
    @Bean
    public JwtDecoder customJwtDecoder(){
        return NimbusJwtDecoder.withJwkSetUri(customJwksUri).build();
    }
    @Bean
    public JwtDecoder googleJwtDecoder(){
        return NimbusJwtDecoder.withIssuerLocation(jwtIssuerUri).build();
    }

    @Bean
    public JwtAuthenticationConverter customJwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaim("roles");
            if (roles == null) {
                return Collections.emptyList();
            }
            return roles.stream()
                    .map(roleName -> {
                        if (roleName.startsWith("ROLE_")) {
                            return roleName;
                        } else {
                            return "ROLE_" + roleName;
                        }
                    })
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtAuthenticationProvider customJwtAuthProvider() {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(customJwtDecoder());
        provider.setJwtAuthenticationConverter(customJwtAuthenticationConverter());
        return provider;
    }
    @Bean
    public JwtAuthenticationConverter googleJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Stream.of(new SimpleGrantedAuthority("ROLE_USER")).collect(Collectors.toList()));
        return converter;
    }
}
