import kotlin.jvm.JvmStatic
import net.minestom.server.MinecraftServer

object Server
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        // Initialize the server
        val minecraftServer = MinecraftServer.init()

        // Start the server
        minecraftServer.start("0.0.0.0", 25565)
    }
}