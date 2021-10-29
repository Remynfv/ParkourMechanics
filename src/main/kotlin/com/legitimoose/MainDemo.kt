package com.legitimoose

import com.legitimoose.commands.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.*
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerLoginEvent
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

// Create the instance
val anvilLoader = AnvilLoader("worlds/worlde")
val instanceContainer = instanceManager.createInstanceContainer(anvilLoader)

object MainDemo
{

    @JvmStatic
    fun main(args: Array<String>)
    {

        instanceContainer.enableAutoChunkLoad(true)

        // Set the ChunkGenerator
        instanceContainer.chunkGenerator = GeneratorDemo()

        // Add an event callback to specify the spawning instance (and the spawn position)
        val globalEventHandler = MinecraftServer.getGlobalEventHandler()
        globalEventHandler.addListener(PlayerLoginEvent::class.java) { event: PlayerLoginEvent ->
            val player = event.player
            event.setSpawningInstance(instanceContainer)
            player.respawnPoint = Pos(0.0, 42.0, 0.0)
        }

        //Commands
        MinecraftServer.getCommandManager().register(TestCommand())
        MinecraftServer.getCommandManager().register(GamemodeCommand())
        MinecraftServer.getCommandManager().register(SummonCommand())
        MinecraftServer.getCommandManager().register(PvpCommand())

        globalEventHandler.addListener(ServerListPingEvent::class.java) { event: ServerListPingEvent ->

            val responseData = event.responseData

            when (event.pingType)
            {
                (ServerListPingType.MODERN_FULL_RGB) -> {
                    responseData.version = "yeeet                                                                        "
                    responseData.description = MiniMessage.get().parse("<rainbow>This is a test!</rainbow>")
                    responseData.favicon = ""
                }
                else -> { }
            }
        }

        globalEventHandler.addListener(PlayerEntityInteractEvent::class.java) { event: PlayerEntityInteractEvent ->
            if (event.hand == Player.Hand.OFF)
                CombatUtils.hit(event.target, event.entity, true)
        }

        globalEventHandler.addListener(EntityAttackEvent::class.java) { event: EntityAttackEvent ->
            CombatUtils.hit(event.target, event.entity)
        }

        ZombieCreature().setInstance(instanceContainer, Pos(0.0, 42.0, 0.0))
        EntityCreature(EntityType.BOAT).setInstance(instanceContainer, Pos(5.0, 42.0, 0.0))


        // Start the server on port 25565
        minecraftServer.start("0.0.0.0", 25565)
    }

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