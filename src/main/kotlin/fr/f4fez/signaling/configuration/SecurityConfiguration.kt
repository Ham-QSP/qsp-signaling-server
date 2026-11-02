package fr.f4fez.signaling.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import reactor.core.publisher.Mono
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration {

@Bean
fun jwtAuthenticationConverter(@Value("\${security.jwt.rolesClaim:roles}") rolesClaim: String): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
    val authoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
        setAuthoritiesClaimName(rolesClaim)
        setAuthorityPrefix("ROLE_")
    }
    return ReactiveJwtAuthenticationConverterAdapter(JwtAuthenticationConverter().apply {
        setJwtGrantedAuthoritiesConverter(authoritiesConverter)
    })
}

@Bean
fun springSecurityFilterChain(
    http: ServerHttpSecurity,
    jwtAuthenticationConverter: Converter<Jwt, Mono<AbstractAuthenticationToken>>,
): SecurityWebFilterChain {
    http
        .csrf { it.disable() }
        .authorizeExchange { exchanges ->
            exchanges
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers("/server/session").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()
                .anyExchange().authenticated()
            }
        .cors(Customizer.withDefaults())
        .oauth2ResourceServer { oauth2 ->
            oauth2.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter) }
        }

        return http.build()
    }
}