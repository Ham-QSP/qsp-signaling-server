package fr.f4fez.signaling.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

@Bean
fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    http
        .csrf { it.disable() }
        .authorizeExchange { exchanges ->
            exchanges
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers("/server/session").permitAll()
                .anyExchange().authenticated()
            }
        .cors(Customizer.withDefaults())
        .oauth2ResourceServer { oauth2 -> oauth2.jwt(Customizer.withDefaults()) }

        return http.build()
    }
}