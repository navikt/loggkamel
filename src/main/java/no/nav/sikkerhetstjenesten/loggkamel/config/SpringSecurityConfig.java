package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandlingConfigurer ->
                        exceptionHandlingConfigurer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer
                        -> httpSecurityOAuth2ResourceServerConfigurer.opaqueToken(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> {
                            authorize.requestMatchers("/monitoring/**").permitAll();
                            authorize.requestMatchers("/api/v1/dev/**").permitAll();
                            authorize.requestMatchers("/api/v1/naisteam/futureNaisteamListEndpoint").authenticated();
                            authorize.requestMatchers("/api/v1/naisteam/**").permitAll();
                            authorize.anyRequest().authenticated();
                        }
                );

        return http.build();
    }

}
