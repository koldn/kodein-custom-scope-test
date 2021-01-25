import io.ktor.http.*
import io.ktor.server.testing.*
import org.apache.commons.lang3.RandomStringUtils.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ServerTest {
    @Test
    fun `Error with missing tenant`() {
        val tenant = randomAlphabetic(10)
        withTestApplication({ configure() }) {
            val call = this.handleRequest(HttpMethod.Get, "/greet/$tenant") { }
            Assertions.assertEquals(HttpStatusCode.InternalServerError,call.response.status())
        }
    }

    @Test
    fun `Successful call after registration`() {
        val tenant1 = randomAlphabetic(10)
        withTestApplication({ configure() }) {
            val registrationCall = this.handleRequest(HttpMethod.Post, "/tenant/$tenant1") { }
            Assertions.assertEquals(HttpStatusCode.NoContent,registrationCall.response.status())
            val call = this.handleRequest(HttpMethod.Get, "/greet/$tenant1") { }
            Assertions.assertEquals(HttpStatusCode.OK,call.response.status())
            Assertions.assertEquals("[$tenant1] Hello from Ktor",call.response.content)
        }
    }

    @Test
    fun `Multiple tenant registrations`() {
        val tenant1 = randomAlphabetic(10)
        val tenant2 = randomAlphabetic(10)
        withTestApplication({ configure() }) {
            this.handleRequest(HttpMethod.Post, "/tenant/$tenant1") { }
            this.handleRequest(HttpMethod.Post, "/tenant/$tenant2") { }
                        val call = this.handleRequest(HttpMethod.Get, "/greet/$tenant1") { }
            Assertions.assertEquals(HttpStatusCode.OK,call.response.status())
            Assertions.assertEquals("[$tenant1] Hello from Ktor",call.response.content)

            val tenant2call = this.handleRequest(HttpMethod.Get, "/greet/$tenant2") { }
            Assertions.assertEquals(HttpStatusCode.OK,call.response.status())
            Assertions.assertEquals("[$tenant2] Hello from Ktor",tenant2call.response.content)
        }
    }

    @Test
    fun `Removed registration`() {
        val tenant1 = randomAlphabetic(10)
        val tenant2 = randomAlphabetic(10)
        withTestApplication({ configure() }) {
            this.handleRequest(HttpMethod.Post, "/tenant/$tenant1") { }
            this.handleRequest(HttpMethod.Post, "/tenant/$tenant2") { }
            val call = this.handleRequest(HttpMethod.Get, "/greet/$tenant1") { }
            Assertions.assertEquals(HttpStatusCode.OK,call.response.status())
            Assertions.assertEquals("[$tenant1] Hello from Ktor",call.response.content)

            val tenant2call = this.handleRequest(HttpMethod.Get, "/greet/$tenant2") { }
            Assertions.assertEquals(HttpStatusCode.OK,call.response.status())
            Assertions.assertEquals("[$tenant2] Hello from Ktor",tenant2call.response.content)

            val deleteCall = this.handleRequest(HttpMethod.Delete,"/tenant/$tenant1") {  }
            Assertions.assertEquals(HttpStatusCode.NoContent,deleteCall.response.status())

            val tenant2callAfterDereg = this.handleRequest(HttpMethod.Get, "/greet/$tenant2") { }
            Assertions.assertEquals(HttpStatusCode.OK,tenant2callAfterDereg.response.status())
        }
    }
}