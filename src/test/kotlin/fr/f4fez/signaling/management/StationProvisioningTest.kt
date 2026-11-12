/*
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>
 */
package fr.f4fez.signaling.management

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals

/**
 * The authentication server (role SYS_AUTH) creates stations and makes users owner of them.
 */
@Testcontainers
@SpringBootTest
class StationProvisioningTest {
    @Autowired
    lateinit var context: ApplicationContext

    private val mapper = ObjectMapper()

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToApplicationContext(context).apply(springSecurity()).configureClient().build()
    }

    private fun WebTestClient.asRole(role: String, subject: String = "tester") =
        mutateWith(mockJwt().authorities(SimpleGrantedAuthority("ROLE_$role")).jwt { it.subject(subject) })

    private fun WebTestClient.asUser(subject: String) = mutateWith(mockJwt().jwt { it.subject(subject) })

    private fun uniqueQrz() = "T" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

    private fun stationBody(qrz: String, subject: String = "OWNER_A") =
        """{"mainQRZ":"$qrz","description":"","grants":[{"subject":"$subject","role":"OWNER"}]}"""

    private fun createStation(role: String, qrz: String): WebTestClient.ResponseSpec =
        client.asRole(role).post().uri("/station")
            .header("Content-Type", "application/json").bodyValue(stationBody(qrz)).exchange()

    private fun postGrant(role: String, stationId: String, subject: String, grantRole: String) =
        client.asRole(role).post().uri("/station/$stationId/grant")
            .header("Content-Type", "application/json")
            .bodyValue("""{"subject":"$subject","role":"$grantRole"}""").exchange()

    private fun grants(stationId: String): JsonNode {
        val body = client.asUser("OWNER_A").get().uri("/station/$stationId/grant").exchange()
            .expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody!!
        return mapper.readTree(body)
    }

    private fun stationId(spec: WebTestClient.ResponseSpec): String =
        mapper.readTree(spec.expectBody(String::class.java).returnResult().responseBody!!).get("id").asString()

    @Test
    fun `SYS_AUTH can create a station`() {
        createStation("SYS_AUTH", uniqueQrz()).expectStatus().isOk
    }

    @Test
    fun `ADMIN can still create a station`() {
        createStation("ADMIN", uniqueQrz()).expectStatus().isOk
    }

    @Test
    fun `a plain user cannot create a station`() {
        client.asUser("someone").post().uri("/station")
            .header("Content-Type", "application/json").bodyValue(stationBody(uniqueQrz())).exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `creating an existing station answers 409 with its id`() {
        val qrz = uniqueQrz()
        val id = stationId(createStation("SYS_AUTH", qrz))

        val body = createStation("SYS_AUTH", qrz).expectStatus().isEqualTo(409)
            .expectBody(String::class.java).returnResult().responseBody!!

        assertEquals(id, mapper.readTree(body).get("stationId").asString())
    }

    @Test
    fun `SYS_AUTH grant creates the grant and calling it twice keeps one grant`() {
        val id = stationId(createStation("SYS_AUTH", uniqueQrz()))

        postGrant("SYS_AUTH", id, "NEW_OWNER", "OWNER").expectStatus().isOk
        postGrant("SYS_AUTH", id, "NEW_OWNER", "OWNER").expectStatus().isOk

        assertEquals(1, grants(id).count { it.get("subject").asString() == "NEW_OWNER" })
    }

    @Test
    fun `a member grant becomes owner and keeps its id`() {
        val id = stationId(createStation("SYS_AUTH", uniqueQrz()))
        val memberGrantId = mapper.readTree(
            postGrant("SYS_AUTH", id, "PROMOTED", "MEMBER").expectStatus().isOk
                .expectBody(String::class.java).returnResult().responseBody!!,
        ).get("id").asString()

        val promoted = mapper.readTree(
            postGrant("SYS_AUTH", id, "PROMOTED", "OWNER").expectStatus().isOk
                .expectBody(String::class.java).returnResult().responseBody!!,
        )

        assertEquals("OWNER", promoted.get("role").asString())
        assertEquals(memberGrantId, promoted.get("id").asString())
    }

    @Test
    fun `other grants of the station are left untouched`() {
        val id = stationId(createStation("SYS_AUTH", uniqueQrz()))

        postGrant("SYS_AUTH", id, "ANOTHER", "OWNER").expectStatus().isOk

        assertEquals("OWNER", grants(id).first { it.get("subject").asString() == "OWNER_A" }.get("role").asString())
    }

    @Test
    fun `ADMIN and the station owner can still create grants`() {
        val id = stationId(createStation("SYS_AUTH", uniqueQrz()))

        postGrant("ADMIN", id, "BY_ADMIN", "MEMBER").expectStatus().isOk
        client.asUser("OWNER_A").post().uri("/station/$id/grant")
            .header("Content-Type", "application/json")
            .bodyValue("""{"subject":"BY_OWNER","role":"MEMBER"}""").exchange().expectStatus().isOk
    }

    @Test
    fun `a plain user who is not owner cannot create grants`() {
        val id = stationId(createStation("SYS_AUTH", uniqueQrz()))

        client.asUser("STRANGER").post().uri("/station/$id/grant")
            .header("Content-Type", "application/json")
            .bodyValue("""{"subject":"STRANGER","role":"OWNER"}""").exchange().expectStatus().isForbidden
    }

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
}
