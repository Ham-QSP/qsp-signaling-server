package fr.f4fez.signaling.management

import fr.f4fez.signaling.error.InvalidRequest
import fr.f4fez.signaling.management.dal.StationGrantRole.MEMBER
import fr.f4fez.signaling.management.dal.StationGrantRole.OWNER
import fr.f4fez.signaling.management.dto.StationCreate
import fr.f4fez.signaling.management.dto.StationGrantUpdate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class StationCreateTest {

    private fun station(vararg grants: StationGrantUpdate, qrz: String = "F4XYZ") =
        StationCreate(qrz, "desc", grants.toList())

    @Test
    fun `accepts a station with an owner`() {
        assertDoesNotThrow {
            station(StationGrantUpdate("a", OWNER), StationGrantUpdate("b", MEMBER)).validate()
        }
    }

    @Test
    fun `rejects an empty grant list`() {
        assertThrows<InvalidRequest> { station().validate() }
    }

    @Test
    fun `rejects grants without owner`() {
        assertThrows<InvalidRequest> { station(StationGrantUpdate("a", MEMBER)).validate() }
    }

    @Test
    fun `rejects duplicate subjects`() {
        assertThrows<InvalidRequest> {
            station(StationGrantUpdate("a", OWNER), StationGrantUpdate("a", MEMBER)).validate()
        }
    }

    @Test
    fun `rejects blank or too long subject and qrz`() {
        assertThrows<InvalidRequest> { station(StationGrantUpdate(" ", OWNER)).validate() }
        assertThrows<InvalidRequest> { station(StationGrantUpdate("a".repeat(101), OWNER)).validate() }
        assertThrows<InvalidRequest> { station(StationGrantUpdate("a", OWNER), qrz = "").validate() }
        assertThrows<InvalidRequest> { station(StationGrantUpdate("a", OWNER), qrz = "X".repeat(21)).validate() }
    }
}
