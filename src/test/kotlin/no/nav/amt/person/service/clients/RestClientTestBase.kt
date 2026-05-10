package no.nav.amt.person.service.clients

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.client.MockRestServiceServer

abstract class RestClientTestBase {
    @Autowired
    lateinit var server: MockRestServiceServer

    @MockkBean
    protected lateinit var tokenClient: MachineToMachineTokenClient

    @BeforeEach
    fun setUpTokenClientMock() {
        every { tokenClient.createMachineToMachineToken(any()) } returns TOKEN_IN_TEST
    }

    companion object {
        const val TOKEN_IN_TEST = "test-token"
    }
}
