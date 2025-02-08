package fr.f4fez.signaling.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "database.postgresql")
class PostgresConfigurationProperties {
    var host: String = "localhost"
    var port: Int = 5432
    var database: String = "qsp"
    var username: String = "qsp"
    var password: String = "pass"

}
