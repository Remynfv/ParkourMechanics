package com.legitimoose

import kotlin.jvm.JvmStatic
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.instance.*
import net.minestom.server.instance.batch.ChunkBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.world.biomes.Biome
import java.util.*

object MainDemo
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        // Initialization
        val minecraftServer = MinecraftServer.init()
        val instanceManager = MinecraftServer.getInstanceManager()

        // Create the instance
        val anvilLoader = AnvilLoader("world")
        val instanceContainer = instanceManager.createInstanceContainer(anvilLoader)

        // Set the ChunkGenerator
        instanceContainer.chunkGenerator = GeneratorDemo()
        instanceContainer.saveChunksToStorage()

        // Add an event callback to specify the spawning instance (and the spawn position)
        val globalEventHandler = MinecraftServer.getGlobalEventHandler()
        globalEventHandler.addListener(PlayerLoginEvent::class.java) { event: PlayerLoginEvent ->
            val player = event.player
            event.setSpawningInstance(instanceContainer)
            player.respawnPoint = Pos(0.0, 42.0, 0.0)
        }

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