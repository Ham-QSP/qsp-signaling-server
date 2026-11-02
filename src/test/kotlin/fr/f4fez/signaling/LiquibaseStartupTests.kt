package fr.f4fez.signaling

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.test.assertTrue

/**
 * Starts the whole application against a fresh PostgreSQL and checks that Liquibase
 * ran at startup (requires spring-boot-starter-liquibase on the classpath).
 */
@Testcontainers
@SpringBootTest
class LiquibaseStartupTests {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.liquibase.url") { postgres.jdbcUrl }
            registry.add("spring.liquibase.user") { postgres.username }
            registry.add("spring.liquibase.password") { postgres.password }
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost:1/jwks" }
        }
    }

    @Test
    fun `liquibase creates the schema at startup`() {
        val tables = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { c ->
            c.createStatement().executeQuery(
                "select table_name from information_schema.tables where table_schema = 'public'"
            ).use { rs -> generateSequence { if (rs.next()) rs.getString(1) else null }.toSet() }
        }
        assertTrue(tables.containsAll(setOf("station", "agent", "databasechangelog")), "found tables: $tables")
    }
}
