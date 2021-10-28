package com.legitimoose

import com.legitimoose.commands.GamemodeCommand
import com.legitimoose.commands.TestCommand
import com.legitimoose.commands.ZombieCreature
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.metadata.other.BoatMeta
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.instance.*
import net.minestom.server.instance.batch.ChunkBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.ping.ServerListPingType
import net.minestom.server.world.biomes.Biome
import java.util.*
import kotlin.math.cos
import kotlin.math.sin


object MainDemo
{
    // Initialization
    val minecraftServer: MinecraftServer = MinecraftServer.init()
    val instanceManager: InstanceManager = MinecraftServer.getInstanceManager()

    // Create the instance
    val anvilLoader = AnvilLoader("worlds/worlde")
    val instanceContainer = instanceManager.createInstanceContainer(anvilLoader)
    
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
            {
                if (event.target is LivingEntity)
                {
                    (event.target as LivingEntity).damage(EntityDamage(event.player), 1f)
                    event.player.sendMessage("yo")
                }
            }
        }

        globalEventHandler.addListener(EntityAttackEvent::class.java) { event: EntityAttackEvent ->
            if (event.target is LivingEntity)
            {
                val attacker = event.entity
                val target = event.target as LivingEntity

                //Damage
                target.damage(EntityDamage(event.entity), 0f)

                //Knockback
                val strength: Float = (0.4f * (if (attacker.isSprinting) 1.5f else 1f)).toFloat()

                target.takeKnockback(strength, sin(attacker.position.yaw * (Math.PI/180)), -cos(attacker.position.yaw * (Math.PI/180)))
                if (attacker is Player)
                    attacker.sendMessage("Stregnth: $strength")
            }
        }

        ZombieCreature().setInstance(instanceContainer, Pos(0.0, 42.0, 0.0))


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