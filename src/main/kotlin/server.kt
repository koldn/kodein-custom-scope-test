import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.kodein.di.*
import org.kodein.di.bindings.Scope
import org.kodein.di.bindings.ScopeRegistry
import org.kodein.di.bindings.StandardScopeRegistry
import java.util.concurrent.ConcurrentHashMap

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") { this.configure() }.start(wait = true)
}
    fun Application.configure() {

        val di = DI {
            bind<GreetMessageSource>() with singleton { HelloFromKtorMessageSource() }
            bind<Greeter>() with scoped(TenantScope).singleton { Greeter(context, instance()) }
        }
        routing {
            get("/greet/{tenantName}") {
                val tenant = call.parameters["tenantName"]
                try {
                    val indexHandler by di.on(tenant!!).instance<Greeter>()
                    val message = indexHandler.index()
                    call.respond(HttpStatusCode.OK, message)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "$tenant not found")
                }
            }

            post("/tenant/{tenantName}") {
                TenantScope.registerTenant(call.parameters["tenantName"]!!)
                call.respond(HttpStatusCode.NoContent)
            }
            delete("/tenant/{tenantName}") {
                TenantScope.removeTenant(call.parameters["tenantName"]!!)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }


object TenantScope : Scope<String> {
    private val scopes = ConcurrentHashMap<String, ScopeRegistry>()

    fun registerTenant(tenantName: String) {
        scopes[tenantName] = StandardScopeRegistry()
    }

    fun removeTenant(tenantName: String) {
        scopes.remove(tenantName)?.clear()
    }

    override fun getRegistry(context: String): ScopeRegistry {
        return scopes[context] ?: error("Tenant with id [$context] is not registered")
    }
}

class Greeter(private val tenantName: String, private val greetMessageSource: GreetMessageSource) {
    fun index(): String {
        val greetMessage = greetMessageSource.getGreetMessage()
        return "[$tenantName] $greetMessage"
    }
}

interface GreetMessageSource {
    fun getGreetMessage(): String
}

class HelloFromKtorMessageSource : GreetMessageSource {
    override fun getGreetMessage(): String = "Hello from Ktor"
}