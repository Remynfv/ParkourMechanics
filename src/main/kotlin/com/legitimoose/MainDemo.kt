package com.legitimoose

import com.legitimoose.blocks.DemoHandler
import com.legitimoose.commands.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.instance.*
import net.minestom.server.instance.batch.ChunkBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.ping.ServerListPingType
import net.minestom.server.world.biomes.Biome
import java.util.*


// Initialization
val minecraftServer: MinecraftServer = MinecraftServer.init()
val instanceManager: InstanceManager = MinecraftServer.getInstanceManager()
val schedulerManager = MinecraftServer.getSchedulerManager()
val globalEventHandler = MinecraftServer.getGlobalEventHandler()

// Create the instance
val anvilLoader = AnvilLoader("worlds/worlde")
val instanceContainer = instanceManager.createInstanceContainer(anvilLoader)

private const val SERVER_ICON = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAQfSURBVHhe7ZtNTBNBFMdnd9tCisoBTZCriCaScDChJpT4wU1vxuhFzp5MPPhx0KABEw968ObZo8YzB2OIUUjkYDh5kOCZgnrho0JLW/e9fW/pTnc6a7sNSOfXvPyntDtt9v+f3ZntYm3nf1eESyrdA6JmilTHbVINVncntTSc/0ON+ox/ooaGzcQotTxs0rbFcu3HBCg5oM4zJgHKBPyj81dOj3gNBdPLX6kVzuXjZ6kVzvTALLU8mnWeMQmoSUCDzk+sUAKkPE32zlHLQ04COx91+55cMAkqdM4zbZ+A3R0AzkdxH5wPO+KDc27Nih9YFj0mciNYWprdngDno7oPmGNAZUoedQo0R3t26bNYQgX3gKw4gSqPZZlmt28UkwBtAhTOs2PslE1akbrj18uaj2l2+0YxCVAmQOX8qvc8cbIDNSqVxSI1PKnBM1pYA0mvERFtvxpMAmoSIJ3j/ZkeOZ8c7kIdnLmPatnBfWg7weeVUhn1w4VHqKX5TVT/U8l5J+P1e2lmErXpfiNiEuAnQOE886T7IurQwkPU4Y1tVJn5I956P5kMjuVycQf1/dAD1PL3Aqp9KoU6tvAMNRNTv1Fp+wTY6LzkfjU8F4fzNBScj6vPySNd6UBl1rawisUiFmO5YxhKBZz/q+cAcfWrwySAVAnMxOBRWipgndvwip25lrKxXqaTWDrHZHaWtrHi7jcqJgGkWpz+FNaXQ17Nbeax3hXKWAw7JztWcs/bUDKJ/g6suPuNikkAqQ9ct6937d6xHSyZO/kiFjvHTsL5G8pxj9RQKuD8DsXv5+2b7VeHSQCp7zxcpa13jR6m7lCyU3Lx6+wszN15/h4Gv+44bsLcYprtV0fbJ8ASlc7A+sm/Tk9XfBzeR7Rq+5UpoY59fIqqWnzxuGR3eNW2o1gNJmg1GFu/ETEJUCXgcS6LytfqmBVrDdUa8FZxTPX6IIzyIq3SVG+jj7GlfmXk76PtV4NJQNQE8EptVayj8kbs/F3xBnVdbKHGzWHhzU1eiBuochIaxSQgagJWfOcDb/cTcEu8Rs2JPGrc9LkZAF6Jm6gmATGxuwPgnpy69+WA00H3DwImAXrn9wutSaBJAOl/ABz14znyV2MSQKpEvl5/0DAJIFWyKjaw9n4WYM4CLcHfAXDvbfj9t3vvfSsxCVA5D6s8qP3jv5kHtISaHQB3Y0NN9c5hfRPLWHufhNZ8A5MA0hryqRLWfpkJlukRNyYBpD58v3264GBFBa7aQsV9rOb+uP+4afsEWOOiggNc/i+LraPeuB/NHUM9I/pQVe6WaHzeE29R+fcB29/H8pZ8XAn/O493dv25uI7q/1YZEyYBVxPZ0EM8JyBLCRjUJIDR/UbYKHH9DiDT9gnQ7oDZ3p9YUWdi4FQrqlW0eQKE+Avg0iUCJBSwYQAAAABJRU5ErkJggg=="

object MainDemo
{
    @JvmStatic
    fun main(args: Array<String>)
    {

        instanceContainer.enableAutoChunkLoad(true)

        // Set the ChunkGenerator
        instanceContainer.chunkGenerator = GeneratorDemo()

        var tnt = Block.TNT
        // Create a new block with the specified handler.
        // Be aware that block objects can be reused, handlers should
        // therefore never assume to be assigned to a single block.
        // Create a new block with the specified handler.
        // Be aware that block objects can be reused, handlers should
        // therefore never assume to be assigned to a single block.
        tnt = tnt.withHandler(DemoHandler())

        //Commands
        MinecraftServer.getCommandManager().register(TestCommand())
        MinecraftServer.getCommandManager().register(GamemodeCommand())
        MinecraftServer.getCommandManager().register(SummonCommand())
        MinecraftServer.getCommandManager().register(PvpCommand())
        MinecraftServer.getCommandManager().register(FillCommand())

        globalEventHandler.addListener(ServerListPingEvent::class.java) { event: ServerListPingEvent ->

            val responseData = event.responseData

            when (event.pingType)
            {
                (ServerListPingType.MODERN_FULL_RGB) -> {
                    responseData.version = "yeeet                                                                        "
                    responseData.description = MiniMessage.get().parse("<rainbow>This is a test!</rainbow>")
                    responseData.favicon = "data:image/png;base64," + SERVER_ICON
                }
                else -> { }
            }
        }

        globalEventHandler.addListener(PlayerEntityInteractEvent::class.java) { event: PlayerEntityInteractEvent ->
            if (event.hand == Player.Hand.OFF)
                CombatUtils.hit(event.target, event.entity, true)
        }

        globalEventHandler.addListener(PlayerBlockPlaceEvent::class.java) { event: PlayerBlockPlaceEvent ->
            if (event.block == Block.TNT)
                event.block = tnt
        }

        globalEventHandler.addListener(EntityAttackEvent::class.java) { event: EntityAttackEvent ->
            CombatUtils.hit(event.target, event.entity)
        }


        MinecraftServer.getConnectionManager().setPlayerProvider(ParkourPlayerProvider())

        ZombieCreature().setInstance(instanceContainer, Pos(0.0, 42.0, 0.0))
        EntityCreature(EntityType.BOAT).setInstance(instanceContainer, Pos(5.0, 42.0, 0.0))



        // Start the server on port 25565
        minecraftServer.start("0.0.0.0", 25565)
    }


    var playersMovedLastTick = mutableListOf<UUID>()

    private class GeneratorDemo : ChunkGenerator
    {
        override fun generateChunkData(batch: ChunkBatch, chunkX: Int, chunkZ: Int)
        {
            // Set chunk blocks
            for (x in 0 until Chunk.CHUNK_SIZE_X)
            {
                for (z in 0 until Chunk.CHUNK_SIZE_Z)
                {
                    for (y in 0..39)
                    {
                        batch.setBlock(x, y, z, Block.STONE)
                    }
                }
            }
        }

        override fun fillBiomes(biomes: Array<Biome>, chunkX: Int, chunkZ: Int)
        {
            Arrays.fill(biomes, Biome.PLAINS)
        }

        override fun getPopulators(): List<ChunkPopulator>?
        {
            return null
        }
    }
}