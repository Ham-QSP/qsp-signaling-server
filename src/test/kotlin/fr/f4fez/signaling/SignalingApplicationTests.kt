package fr.f4fez.signaling

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.liquibase.enabled=false"])
class SignalingApplicationTests {

    @Test
    fun contextLoads() {
    }

}
